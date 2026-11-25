package com.ejemplo.tienda.inventory;

/**
 * The discount rule, and only the discount rule. It is the one class the sales team's
 * changes should touch.
 *
 * <p>RN-1: if cantidad &gt;= umbral (u), the order gets porcentaje (d) % off; otherwise 0 %.
 * <p>RN-2: total = cantidad x precio x (100 - descuento) / 100, in whole pesos.
 */
public final class DescuentoPorVolumen {

    private final int umbral;
    private final int porcentaje;

    public DescuentoPorVolumen(int umbral, int porcentaje) {
        if (umbral <= 0 || porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException("Umbral o porcentaje invalidos: u=" + umbral + " d=" + porcentaje);
        }
        this.umbral = umbral;
        this.porcentaje = porcentaje;
    }

    /** RN-1. */
    public int porcentajePara(int cantidad) {
        return cantidad >= umbral ? porcentaje : 0;
    }

    /** RN-2. */
    public long total(int cantidad, long precio) {
        return cantidad * precio * (100 - porcentajePara(cantidad)) / 100;
    }
}
