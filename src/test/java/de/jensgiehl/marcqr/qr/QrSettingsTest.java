package de.jensgiehl.marcqr.qr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrSettingsTest {

    @Test
    void rejectsColorsWithInsufficientContrast() {
        assertThatThrownBy(() -> new QrSettings(300, "#eeeeee", "#ffffff"))
                .isInstanceOf(QrValidationException.class)
                .hasMessageContaining("Farbkontrast");
    }

    @Test
    void usesSensibleDefaultCornerRadii() {
        var settings = new QrSettings(300, "#000000", "#ffffff");

        assertThat(settings.cornerRadius()).isZero();
        assertThat(settings.imageCornerRadius()).isEqualTo(4);
    }

    @Test
    void rejectsCornerRadiusAboveMaximum() {
        assertThatThrownBy(() -> new QrSettings(300, "#000000", "#ffffff", 51))
                .isInstanceOf(QrValidationException.class)
                .hasMessageContaining("Eckenrundung");
    }

    @Test
    void rejectsImageCornerRadiusAboveMaximum() {
        assertThatThrownBy(() -> new QrSettings(300, "#000000", "#ffffff", 0, 26))
                .isInstanceOf(QrValidationException.class)
                .hasMessageContaining("Bild-Eckenrundung");
    }
}
