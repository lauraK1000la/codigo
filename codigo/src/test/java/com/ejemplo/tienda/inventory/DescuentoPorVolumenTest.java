package com.ejemplo.tienda.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** RN-1 and RN-2 with the numbers of row 1: Mouse, $45.000, u = 5, d = 10 %. */
class DescuentoPorVolumenTest {

    private final DescuentoPorVolumen descuento = new DescuentoPorVolumen(5, 10);

    @Test
    void justBelowTheThresholdThereIsNoDiscount() {
        assertThat(descuento.porcentajePara(4)).isEqualTo(0);
        assertThat(descuento.total(4, 45_000)).isEqualTo(180_000L);
    }

    @Test
    void theThresholdItselfAlreadyGetsTheDiscountBecauseTheRuleIsGreaterOrEqual() {
        assertThat(descuento.porcentajePara(5)).isEqualTo(10);
        // 5 x 45000 x 90 / 100
        assertThat(descuento.total(5, 45_000)).isEqualTo(202_500L);
    }

    @Test
    void oneUnitHasNoDiscount() {
        assertThat(descuento.total(1, 45_000)).isEqualTo(45_000L);
    }
}
