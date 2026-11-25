package com.ejemplo.tienda.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Something that already happened. Events are named in the past tense and are
 * the shared contract between modules: modules know the events, never each other.
 */
public interface Event {

    UUID eventId();

    Instant occurredAt();

    /** Every event of this shop is about one order (numeric id: 1, 2, 3...). */
    long orderId();
}
