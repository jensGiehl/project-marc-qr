package de.jensgiehl.marcqr.qr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrSettingsTest {

    @Test
    void rejectsColorsWithInsufficientContrast() {
        assertThatThrownBy(() -> new QrSettings(300, "#eeeeee", "#ffffff"))
                .isInstanceOf(QrValidationException.class)
                .hasMessageContaining("Farbkontrast");
    }
}
