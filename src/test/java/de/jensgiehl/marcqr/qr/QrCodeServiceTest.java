package de.jensgiehl.marcqr.qr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeServiceTest {

    private final QrCodeService service = new QrCodeService();

    @ParameterizedTest
    @ValueSource(ints = {0, 30, 50})
    void generatesScannablePngForEveryRelevantCornerRadius(int cornerRadius) throws Exception {
        String content = "https://example.org";
        byte[] imageBytes = service.generate(
                content, new QrSettings(320, "#132238", "#ffffff", cornerRadius), null);
        var image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        var bitmap = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels)));

        assertThat(imageBytes).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
        assertThat(imageBytes.length).isGreaterThan(1_000);
        assertThat(image.getWidth()).isEqualTo(320);
        assertThat(image.getHeight()).isEqualTo(320);
        assertThat(new MultiFormatReader().decode(bitmap).getText()).isEqualTo(content);
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

    @Test
    void cornerRadiusChangesRenderedImage() {
        byte[] square = service.generate(
                "Sichtbarer Unterschied", new QrSettings(320, "#000000", "#ffffff", 0), null);
        byte[] rounded = service.generate(
                "Sichtbarer Unterschied", new QrSettings(320, "#000000", "#ffffff", 50), null);

        assertThat(rounded).isNotEqualTo(square);
    }

    @Test
    void imageCornerRadiusCreatesTransparentOuterCorners() throws Exception {
        byte[] imageBytes = service.generate(
                "Transparente Bildecken", new QrSettings(320, "#000000", "#ffffff", 0, 4), null);
        var image = ImageIO.read(new ByteArrayInputStream(imageBytes));

        assertThat(image.getColorModel().hasAlpha()).isTrue();
        assertThat(image.getRGB(0, 0) >>> 24).isZero();
        assertThat(image.getRGB(image.getWidth() / 2, image.getHeight() / 2) >>> 24).isEqualTo(255);
    }
}
