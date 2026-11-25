package com.ejemplo.tienda.inventory;

/**
 * The discount rule, and only the discount rule. It is the one class the sales team's
 * changes should touch.
 *
 * <p>RN-1: if cantidad &gt;= umbral (u), the order gets porcentaje (d) % off; otherwise 0 %.
 * <p>Hoja 2 (Q1): new tier, if cantidad &gt;= 3u the discount is d + 5 points instead of d.
 * <p>RN-2: total = cantidad x precio x (100 - descuento) / 100, in whole pesos.
 */
public final class DescuentoPorVolumen {

    private static final int MULTIPLO_TRAMO_ALTO = 3;
    private static final int PUNTOS_EXTRA_TRAMO_ALTO = 5;

    private final int umbral;
    private final int porcentaje;

    public DescuentoPorVolumen(int umbral, int porcentaje) {
        if (umbral <= 0 || porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("Umbral o porcentaje invalidos: u=" + umbral + " d=" + porcentaje);
        }
        this.umbral = umbral;
        this.porcentaje = porcentaje;
    }

    /** RN-1, with the Hoja 2 tier. */
    public int porcentajePara(int cantidad) {
        if (cantidad >= MULTIPLO_TRAMO_ALTO * umbral) {
            return porcentaje + PUNTOS_EXTRA_TRAMO_ALTO;
        }
        return cantidad >= umbral ? porcentaje : 0;
    }

    /** RN-2. */
    public long total(int cantidad, long precio) {
        return cantidad * precio * (100 - porcentajePara(cantidad)) / 100;
    }
}
