package com.ejemplo.tienda.event;

import java.time.Instant;
import java.util.UUID;

/** Inventory could not keep the units for an order. Published by inventory. */
public record StockRejected(
    UUID eventId,
    Instant occurredAt,
    long orderId,
    String customerId,
    String reason
) implements Event {

    public static StockRejected of(long orderId, String customerId, String reason) {
        return new StockRejected(UUID.randomUUID(), Instant.now(), orderId, customerId, reason);
    }
}
