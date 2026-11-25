package com.ejemplo.tienda.support;

import com.ejemplo.tienda.bus.EventBus;
import com.ejemplo.tienda.event.Event;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * TEST DOUBLE, not production code. It plays the broker inside one thread so the
 * business tests run in milliseconds, without Docker, and in a fixed order.
 *
 * <p>It keeps the semantics that matter: {@code publish} only queues; {@code processAll}
 * is the "mail carrier"; each consumer name gets ITS OWN copy of every event it
 * asked for; a failing consumer does not stop the others.
 */
public class InMemoryEventBus implements EventBus {

    private record Registration(Class<? extends Event> type, Consumer<Event> handler) {
    }

    private final Deque<Event> pending = new ArrayDeque<>();
    private final Map<String, List<Registration>> consumers = new LinkedHashMap<>();
    private final List<Event> failed = new ArrayList<>();

    @Override
    public void publish(Event event) {
        pending.addLast(event);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> void subscribe(String consumerName, Class<T> type, Consumer<T> handler) {
        consumers.computeIfAbsent(consumerName, name -> new ArrayList<>())
            .add(new Registration(type, event -> handler.accept((T) event)));
    }

    /** Delivers every pending event, including the ones published while delivering. */
    public void processAll() {
        while (!pending.isEmpty()) {
            Event event = pending.removeFirst();
            for (List<Registration> registrations : consumers.values()) {
                deliver(event, registrations);
            }
        }
    }

    // One consumer = one "queue": if one of its handlers fails, the message goes to the
    // failed list (the dead letters) and the other consumers still get their copy
    private void deliver(Event event, List<Registration> registrations) {
        try {
            for (Registration registration : registrations) {
                if (registration.type().isInstance(event)) {
                    registration.handler().accept(event);
                }
            }
        } catch (RuntimeException error) {
            failed.add(event);
        }
    }

    public int pendingCount() {
        return pending.size();
    }

    public List<Event> deadLetters() {
        return List.copyOf(failed);
    }
}
