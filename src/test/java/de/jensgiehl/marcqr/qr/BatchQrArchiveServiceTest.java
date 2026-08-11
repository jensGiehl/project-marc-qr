package de.jensgiehl.marcqr.qr;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class BatchQrArchiveServiceTest {

    private final BatchQrArchiveService service = new BatchQrArchiveService();

    @Test
    void createsZipAndAddsSuffixesForDuplicateFilenames() throws Exception {
        String image = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        List<BatchQrItem> items = List.of(
                new BatchQrItem("a", "Text", "a", image, "exampleorg.png"),
                new BatchQrItem("b", "Text", "b", image, "exampleorg.png"));

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(service.create(items)))) {
            assertThat(zip.getNextEntry().getName()).isEqualTo("exampleorg.png");
            assertThat(zip.readAllBytes()).containsExactly(1, 2, 3);
            assertThat(zip.getNextEntry().getName()).isEqualTo("exampleorg-2.png");
            assertThat(zip.readAllBytes()).containsExactly(1, 2, 3);
            assertThat(zip.getNextEntry()).isNull();
        }
    }
}
