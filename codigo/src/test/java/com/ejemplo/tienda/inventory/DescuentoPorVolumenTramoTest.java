package com.ejemplo.tienda.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Hoja 2 (Q1): from 3u units the discount is d + 5. Row 1: u = 5, d = 10 %, so 15 units get 15 %. */
class DescuentoPorVolumenTramoTest {

    private final DescuentoPorVolumen descuento = new DescuentoPorVolumen(5, 10);

    @Test
    void fromThreeTimesTheThresholdTheDiscountIsDPlusFive() {
        assertThat(descuento.porcentajePara(15)).isEqualTo(15);
        // 15 x 45000 x 85 / 100
        assertThat(descuento.total(15, 45_000)).isEqualTo(573_750L);
    }

    @Test
    void justBelowTheNewTierTheDiscountIsStillD() {
        assertThat(descuento.porcentajePara(14)).isEqualTo(10);
        assertThat(descuento.total(14, 45_000)).isEqualTo(567_000L);
    }

    @Test
    void theLastOrderOfTheHoja1SampleNowGetsTheNewTier() {
        assertThat(descuento.total(31, 45_000)).isEqualTo(1_185_750L);
    }
}
