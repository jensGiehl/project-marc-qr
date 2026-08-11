package de.jensgiehl.marcqr.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class QrCodeService {

    public static final int MAX_LOGO_BYTES = 2 * 1024 * 1024;
    private static final int QR_MARGIN = 2;
    private static final int FINDER_PATTERN_SIZE = 7;

    public byte[] generate(String content, QrSettings settings, byte[] logoBytes) {
        validateContent(content, logoBytes != null && logoBytes.length > 0);
        BufferedImage logo = readLogo(logoBytes);

        try {
            var hints = Map.of(
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.ERROR_CORRECTION,
                    logo == null ? ErrorCorrectionLevel.M : ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN, QR_MARGIN
            );
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
            BufferedImage image = render(matrix, settings);
            if (logo != null) {
                overlayLogo(image, logo);
            }
            return toPng(image);
        } catch (WriterException e) {
            throw new QrValidationException("Der Inhalt ist für einen QR-Code zu lang.", e);
        }
    }

    private void validateContent(String content, boolean withLogo) {
        if (content == null || content.isBlank()) {
            throw new QrValidationException("Bitte einen Inhalt für den QR-Code eingeben.");
        }
        QrCapacity.CapacityResult capacity = QrCapacity.inspect(content, withLogo);
        if (capacity.exceedsMaximum()) {
            throw new QrValidationException(
                    "Der Inhalt verwendet %d %s. Mit diesen Einstellungen sind höchstens %d möglich."
                            .formatted(capacity.used(), capacity.unit(), capacity.maximum()));
        }
    }

    private BufferedImage readLogo(byte[] logoBytes) {
        if (logoBytes == null || logoBytes.length == 0) {
            return null;
        }
        if (logoBytes.length > MAX_LOGO_BYTES) {
            throw new QrValidationException("Das Logo darf höchstens 2 MB groß sein.");
        }
        try {
            BufferedImage logo = ImageIO.read(new ByteArrayInputStream(logoBytes));
            if (logo == null) {
                throw new QrValidationException("Das Logo ist kein unterstütztes Bild.");
            }
            if (logo.getWidth() > 4_000 || logo.getHeight() > 4_000) {
                throw new QrValidationException("Das Logo darf höchstens 4.000 × 4.000 Pixel groß sein.");
            }
            return logo;
        } catch (IOException e) {
            throw new QrValidationException("Das Logo konnte nicht gelesen werden.", e);
        }
    }

    private BufferedImage render(BitMatrix matrix, QrSettings settings) {
        Color foreground = Color.decode(settings.foreground());
        Color background = Color.decode(settings.background());
        BufferedImage image = new BufferedImage(settings.size(), settings.size(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            renderImageBackground(graphics, settings, background);
            graphics.setColor(foreground);
            if (settings.cornerRadius() == 0) {
                renderSquareModules(matrix, settings.size(), graphics);
            } else {
                renderRoundedModules(matrix, settings, graphics, foreground, background);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void renderImageBackground(Graphics2D graphics, QrSettings settings, Color background) {
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(new Color(background.getRed(), background.getGreen(), background.getBlue(), 0));
        graphics.fillRect(0, 0, settings.size(), settings.size());
        graphics.setColor(background);
        if (settings.imageCornerRadius() == 0) {
            graphics.fillRect(0, 0, settings.size(), settings.size());
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double arcSize = settings.size() * settings.imageCornerRadius() / 50.0;
            fillRoundedSquare(graphics, 0, 0, settings.size(), arcSize);
        }
        graphics.setComposite(AlphaComposite.SrcOver);
    }

    private void renderSquareModules(BitMatrix matrix, int size, Graphics2D graphics) {
        double moduleSize = (double) size / matrix.getWidth();
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                if (matrix.get(x, y)) {
                    int left = (int) Math.floor(x * moduleSize);
                    int top = (int) Math.floor(y * moduleSize);
                    int right = (int) Math.ceil((x + 1) * moduleSize);
                    int bottom = (int) Math.ceil((y + 1) * moduleSize);
                    graphics.fillRect(left, top, right - left, bottom - top);
                }
            }
        }
    }

    private void renderRoundedModules(BitMatrix matrix, QrSettings settings, Graphics2D graphics,
                                      Color foreground, Color background) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double moduleSize = (double) settings.size() / matrix.getWidth();
        double arcSize = moduleSize * settings.cornerRadius() / 50.0;
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                if (matrix.get(x, y) && !isFinderPattern(x, y, matrix.getWidth())) {
                    graphics.fill(new RoundRectangle2D.Double(
                            x * moduleSize, y * moduleSize, moduleSize, moduleSize, arcSize, arcSize));
                }
            }
        }
        renderFinderPatterns(matrix.getWidth(), moduleSize, settings.cornerRadius(),
                graphics, foreground, background);
    }

    private void renderFinderPatterns(int matrixSize, double moduleSize, int cornerRadius,
                                      Graphics2D graphics, Color foreground, Color background) {
        int farFinderStart = matrixSize - QR_MARGIN - FINDER_PATTERN_SIZE;
        renderFinderPattern(QR_MARGIN, QR_MARGIN, moduleSize, cornerRadius, graphics, foreground, background);
        renderFinderPattern(farFinderStart, QR_MARGIN, moduleSize, cornerRadius, graphics, foreground, background);
        renderFinderPattern(QR_MARGIN, farFinderStart, moduleSize, cornerRadius, graphics, foreground, background);
    }

    private void renderFinderPattern(int moduleX, int moduleY, double moduleSize, int cornerRadius,
                                     Graphics2D graphics, Color foreground, Color background) {
        double rounding = (double) cornerRadius / QrSettings.MAX_CORNER_RADIUS;
        double x = moduleX * moduleSize;
        double y = moduleY * moduleSize;

        graphics.setColor(foreground);
        fillRoundedSquare(graphics, x, y, 7 * moduleSize, 3 * moduleSize * rounding);
        graphics.setColor(background);
        fillRoundedSquare(graphics, x + moduleSize, y + moduleSize,
                5 * moduleSize, 2.4 * moduleSize * rounding);
        graphics.setColor(foreground);
        fillRoundedSquare(graphics, x + 2 * moduleSize, y + 2 * moduleSize,
                3 * moduleSize, 2 * moduleSize * rounding);
    }

    private void fillRoundedSquare(Graphics2D graphics, double x, double y, double size, double arcSize) {
        graphics.fill(new RoundRectangle2D.Double(x, y, size, size, arcSize, arcSize));
    }

    private boolean isFinderPattern(int x, int y, int matrixSize) {
        int farFinderStart = matrixSize - QR_MARGIN - FINDER_PATTERN_SIZE;
        boolean atTop = y >= QR_MARGIN && y < QR_MARGIN + FINDER_PATTERN_SIZE;
        boolean atLeft = x >= QR_MARGIN && x < QR_MARGIN + FINDER_PATTERN_SIZE;
        boolean atRight = x >= farFinderStart && x < farFinderStart + FINDER_PATTERN_SIZE;
        boolean atBottom = y >= farFinderStart && y < farFinderStart + FINDER_PATTERN_SIZE;
        return atTop && (atLeft || atRight) || atBottom && atLeft;
    }

    private void overlayLogo(BufferedImage qrCode, BufferedImage logo) {
        int canvasSize = Math.max(44, Math.round(qrCode.getWidth() * 0.22f));
        int logoSize = Math.max(32, Math.round(qrCode.getWidth() * 0.16f));
        int canvasX = (qrCode.getWidth() - canvasSize) / 2;
        int canvasY = (qrCode.getHeight() - canvasSize) / 2;
        int logoX = (qrCode.getWidth() - logoSize) / 2;
        int logoY = (qrCode.getHeight() - logoSize) / 2;

        Graphics2D graphics = qrCode.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setColor(Color.WHITE);
            graphics.fill(new RoundRectangle2D.Float(canvasX, canvasY, canvasSize, canvasSize,
                    canvasSize * 0.22f, canvasSize * 0.22f));

            double scale = Math.min((double) logoSize / logo.getWidth(), (double) logoSize / logo.getHeight());
            int width = Math.max(1, (int) Math.round(logo.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(logo.getHeight() * scale));
            int x = logoX + (logoSize - width) / 2;
            int y = logoY + (logoSize - height) / 2;
            graphics.drawImage(logo, x, y, width, height, null);
        } finally {
            graphics.dispose();
        }
    }

    private byte[] toPng(BufferedImage image) {
        try (var output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("QR-Code konnte nicht als PNG geschrieben werden.", e);
        }
    }
}
