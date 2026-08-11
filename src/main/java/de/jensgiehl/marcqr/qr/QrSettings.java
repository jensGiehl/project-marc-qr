package de.jensgiehl.marcqr.qr;

import java.awt.Color;
import java.util.regex.Pattern;

public record QrSettings(int size, String foreground, String background,
                         int cornerRadius, int imageCornerRadius) {

    public static final int MIN_SIZE = 160;
    public static final int MAX_SIZE = 1_200;
    public static final int DEFAULT_CORNER_RADIUS = 0;
    public static final int MAX_CORNER_RADIUS = 50;
    public static final int DEFAULT_IMAGE_CORNER_RADIUS = 4;
    public static final int MAX_IMAGE_CORNER_RADIUS = 25;
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    public QrSettings(int size, String foreground, String background) {
        this(size, foreground, background, DEFAULT_CORNER_RADIUS, DEFAULT_IMAGE_CORNER_RADIUS);
    }

    public QrSettings(int size, String foreground, String background, int cornerRadius) {
        this(size, foreground, background, cornerRadius, DEFAULT_IMAGE_CORNER_RADIUS);
    }

    public QrSettings {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new QrValidationException("Die Größe muss zwischen %d und %d Pixeln liegen."
                    .formatted(MIN_SIZE, MAX_SIZE));
        }
        if (foreground == null || !HEX_COLOR.matcher(foreground).matches()
                || background == null || !HEX_COLOR.matcher(background).matches()) {
            throw new QrValidationException("Farben müssen im Format #RRGGBB angegeben werden.");
        }
        if (foreground.equalsIgnoreCase(background)) {
            throw new QrValidationException("Vorder- und Hintergrundfarbe müssen verschieden sein.");
        }
        if (contrastRatio(foreground, background) < 3.0) {
            throw new QrValidationException("Der Farbkontrast ist zu gering. Bitte deutlichere Farben wählen.");
        }
        if (cornerRadius < 0 || cornerRadius > MAX_CORNER_RADIUS) {
            throw new QrValidationException("Die QR-Eckenrundung muss zwischen 0 und %d Prozent liegen."
                    .formatted(MAX_CORNER_RADIUS));
        }
        if (imageCornerRadius < 0 || imageCornerRadius > MAX_IMAGE_CORNER_RADIUS) {
            throw new QrValidationException("Die Bild-Eckenrundung muss zwischen 0 und %d Prozent liegen."
                    .formatted(MAX_IMAGE_CORNER_RADIUS));
        }
    }

    private static double contrastRatio(String first, String second) {
        double firstLuminance = luminance(Color.decode(first));
        double secondLuminance = luminance(Color.decode(second));
        return (Math.max(firstLuminance, secondLuminance) + 0.05)
                / (Math.min(firstLuminance, secondLuminance) + 0.05);
    }

    private static double luminance(Color color) {
        double red = linearChannel(color.getRed() / 255.0);
        double green = linearChannel(color.getGreen() / 255.0);
        double blue = linearChannel(color.getBlue() / 255.0);
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double linearChannel(double channel) {
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
