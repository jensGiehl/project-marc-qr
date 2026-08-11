package de.jensgiehl.marcqr.qr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BatchQrServiceTest {

    private final BatchQrService service = new BatchQrService(new QrCodeService(), new QrFilenameService());

    @Test
    void skipsEmptyLinesAndDetectsContentTypes() {
        var items = service.generate("example.org/path\n\nmail@example.org\nHallo", new QrSettings(200, "#000000", "#ffffff"), null);

        assertThat(items).extracting(BatchQrItem::type)
                .containsExactly("Webseite", "E-Mail", "Text");
        assertThat(items.getFirst().payload()).isEqualTo("https://example.org/path");
        assertThat(items.get(1).payload()).isEqualTo("mailto:mail@example.org");
        assertThat(items).allSatisfy(item -> assertThat(item.imageBase64()).isNotBlank());
        assertThat(items).extracting(BatchQrItem::filename)
                .containsExactly("exampleorgpath.png", "mailexampleorg.png", "Hallo.png");
    }
}
