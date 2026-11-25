package com.ejemplo.tienda.orders;

/** One order of one product. The total (whole pesos) is known only after inventory answers. */
public record Order(
    long id,
    String clienteId,
    long productoId,
    int cantidad,
    OrderStatus estado,
    long total,
    String nota
) {

    Order confirmed(long newTotal) {
        return new Order(id, clienteId, productoId, cantidad, OrderStatus.CONFIRMADO, newTotal, null);
    }

    Order rejected(String reason) {
        return new Order(id, clienteId, productoId, cantidad, OrderStatus.RECHAZADO, 0, reason);
    }
}
