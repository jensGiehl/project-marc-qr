package de.jensgiehl.marcqr.qr;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class QrCapacity {

    private static final Pattern NUMERIC = Pattern.compile("[0-9]*");
    private static final Pattern ALPHANUMERIC = Pattern.compile("[0-9A-Z $%*+./:-]*");

    private QrCapacity() {
    }

    public static CapacityResult inspect(String content, boolean withLogo) {
        var level = withLogo ? CapacityLevel.HIGH : CapacityLevel.MEDIUM;
        if (NUMERIC.matcher(content).matches()) {
            return new CapacityResult(content.length(), level.numeric(), "Ziffern");
        }
        if (ALPHANUMERIC.matcher(content).matches()) {
            return new CapacityResult(content.length(), level.alphanumeric(), "Zeichen");
        }
        return new CapacityResult(content.getBytes(StandardCharsets.UTF_8).length, level.bytes(), "UTF-8-Bytes");
    }

    public record CapacityResult(int used, int maximum, String unit) {

        public boolean exceedsMaximum() {
            return used > maximum;
        }
    }

    private enum CapacityLevel {
        MEDIUM(5_596, 3_391, 2_331),
        HIGH(3_057, 1_852, 1_273);

        private final int numeric;
        private final int alphanumeric;
        private final int bytes;

        CapacityLevel(int numeric, int alphanumeric, int bytes) {
            this.numeric = numeric;
            this.alphanumeric = alphanumeric;
            this.bytes = bytes;
        }

        int numeric() {
            return numeric;
        }

        int alphanumeric() {
            return alphanumeric;
        }

        int bytes() {
            return bytes;
        }
    }
}
