package com.ejemplo.tienda.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Inventory kept the units for an order. Published by inventory.
 * It carries everything the consumers need (total in whole pesos, customer),
 * so nobody has to call back.
 */
public record StockReserved(
    UUID eventId,
    Instant occurredAt,
    long orderId,
    long productId,
    int quantity,
    long total,
    String customerId
) implements Event {

    public static StockReserved of(long orderId, long productId, int quantity,
                                   long total, String customerId) {
        return new StockReserved(UUID.randomUUID(), Instant.now(),
            orderId, productId, quantity, total, customerId);
    }
}
