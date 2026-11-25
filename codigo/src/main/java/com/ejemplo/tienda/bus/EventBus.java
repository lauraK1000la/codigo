package com.ejemplo.tienda.bus;

import com.ejemplo.tienda.event.Event;
import java.util.function.Consumer;

/**
 * The port. Modules publish events and subscribe to event types through this
 * interface and know nothing about how events travel (RabbitMQ, Kafka, memory...).
 */
public interface EventBus {

    /** Announces that something happened. The publisher does not wait for any consumer. */
    void publish(Event event);

    /**
     * Registers a handler for events of this type. {@code Event.class} means "every event".
     *
     * <p>{@code consumerName} says WHO is listening ("billing", "notifications"...).
     * Each consumer name gets its own copy of every event it asked for, in its own
     * queue, so a slow or failing consumer does not affect the others.
     */
    <T extends Event> void subscribe(String consumerName, Class<T> type, Consumer<T> handler);
}
