package de.jensgiehl.marcqr.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
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

    public byte[] generate(String content, QrSettings settings, byte[] logoBytes) {
        validateContent(content, logoBytes != null && logoBytes.length > 0);
        BufferedImage logo = readLogo(logoBytes);

        try {
            var hints = Map.of(
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.ERROR_CORRECTION,
                    logo == null ? ErrorCorrectionLevel.M : ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN, 2
            );
            BitMatrix matrix = new QRCodeWriter().encode(
                    content, BarcodeFormat.QR_CODE, settings.size(), settings.size(), hints);
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
        int foreground = Color.decode(settings.foreground()).getRGB();
        int background = Color.decode(settings.background()).getRGB();
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                image.setRGB(x, y, matrix.get(x, y) ? foreground : background);
            }
        }
        return image;
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
