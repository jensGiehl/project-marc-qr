package de.jensgiehl.marcqr.qr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QrFilenameServiceTest {

    private final QrFilenameService service = new QrFilenameService();

    @Test
    void createsFilenameFromContentWithoutSchemeOrSpecialCharacters() {
        assertThat(service.create("https://www.example.org/path?q=Marc QR"))
                .isEqualTo("wwwexampleorgpathqMarcQR.png");
        assertThat(service.create("hallo@example.org"))
                .isEqualTo("halloexampleorg.png");
    }

    @Test
    void createsSafeFallbackAndAvoidsReservedWindowsNames() {
        assertThat(service.create("https://..."))
                .isEqualTo("qr-code.png");
        assertThat(service.create("CON"))
                .isEqualTo("qr-CON.png");
    }
}
