package de.jensgiehl.marcqr.qr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeServiceTest {

    private final QrCodeService service = new QrCodeService();

    @Test
    void generatesPngWithRequestedDimensions() {
        byte[] image = service.generate("https://example.org", new QrSettings(320, "#132238", "#ffffff"), null);

        assertThat(image).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
        assertThat(image.length).isGreaterThan(1_000);
    }

    @Test
    void rejectsEmptyContent() {
        assertThatThrownBy(() -> service.generate(" ", new QrSettings(320, "#000000", "#ffffff"), null))
                .isInstanceOf(QrValidationException.class)
                .hasMessageContaining("Inhalt");
    }

    @Test
    void rejectsContentAboveQrCapacity() {
        String content = "ä".repeat(1_200);

        assertThatThrownBy(() -> service.generate(content, new QrSettings(320, "#000000", "#ffffff"), null))
                .isInstanceOf(QrValidationException.class)
                .hasMessageContaining("höchstens");
    }
}
