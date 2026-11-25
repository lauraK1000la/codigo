package com.ejemplo.tienda.event;

import java.time.Instant;
import java.util.UUID;

/** A customer asked for a product. Published by orders. */
public record OrderPlaced(
    UUID eventId,
    Instant occurredAt,
    long orderId,
    long productId,
    int quantity,
    String customerId
) implements Event {

    public static OrderPlaced of(long orderId, long productId, int quantity, String customerId) {
        return new OrderPlaced(UUID.randomUUID(), Instant.now(),
            orderId, productId, quantity, customerId);
    }
}
