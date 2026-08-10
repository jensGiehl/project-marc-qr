package de.jensgiehl.marcqr.qr;

import java.awt.Color;
import java.util.regex.Pattern;

public record QrSettings(int size, String foreground, String background) {

    public static final int MIN_SIZE = 160;
    public static final int MAX_SIZE = 1_200;
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

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
