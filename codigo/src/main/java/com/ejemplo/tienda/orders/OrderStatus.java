package com.ejemplo.tienda.orders;

/** An order is born PENDIENTE and ends CONFIRMADO or RECHAZADO when inventory answers. */
public enum OrderStatus {
    PENDIENTE,
    CONFIRMADO,
    RECHAZADO
}
