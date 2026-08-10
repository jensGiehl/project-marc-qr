package de.jensgiehl.marcqr.qr;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QrCapacityTest {

    @Test
    void recognizesNumericCapacityWithoutLogo() {
        var capacity = QrCapacity.inspect("123456", false);

        assertThat(capacity.used()).isEqualTo(6);
        assertThat(capacity.maximum()).isEqualTo(5_596);
        assertThat(capacity.unit()).isEqualTo("Ziffern");
    }

    @Test
    void usesUtf8BytesAndReducedCapacityWithLogo() {
        var capacity = QrCapacity.inspect("Grüße", true);

        assertThat(capacity.used()).isEqualTo(7);
        assertThat(capacity.maximum()).isEqualTo(1_273);
        assertThat(capacity.unit()).isEqualTo("UTF-8-Bytes");
    }
}
