package com.ejemplo.tienda.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejemplo.tienda.event.Event;
import com.ejemplo.tienda.event.OrderPlaced;
import com.ejemplo.tienda.event.StockReserved;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryEventBusTest {

    private final InMemoryEventBus bus = new InMemoryEventBus();

    @Test
    void publishOnlyQueuesUntilProcessAll() {
        List<Long> received = new ArrayList<>();
        bus.subscribe("a", OrderPlaced.class, e -> received.add(e.orderId()));

        bus.publish(OrderPlaced.of(1, 1, 1, "C-001"));

        assertThat(received).isEmpty();
        bus.processAll();
        assertThat(received).containsExactly(1L);
    }

    @Test
    void eachConsumerGetsItsOwnCopyAndOnlyTheTypesItAskedFor() {
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();
        bus.subscribe("a", OrderPlaced.class, e -> a.add("placed"));
        bus.subscribe("b", OrderPlaced.class, e -> b.add("placed"));
        bus.subscribe("b", StockReserved.class, e -> b.add("reserved"));

        bus.publish(OrderPlaced.of(1, 1, 1, "C-001"));
        bus.publish(StockReserved.of(1, 1, 1, 100, "C-001"));
        bus.processAll();

        assertThat(a).containsExactly("placed");
        assertThat(b).containsExactly("placed", "reserved");
    }

    @Test
    void aFailingConsumerDoesNotStopTheOthers() {
        List<String> good = new ArrayList<>();
        bus.subscribe("bad", OrderPlaced.class, e -> {
            throw new IllegalStateException("boom");
        });
        bus.subscribe("good", OrderPlaced.class, e -> good.add("ok"));

        bus.publish(OrderPlaced.of(1, 1, 1, "C-001"));
        bus.processAll();

        assertThat(good).containsExactly("ok");
        assertThat(bus.deadLetters()).hasSize(1);
    }

    @Test
    void eventsPublishedWhileDeliveringAreAlsoDelivered() {
        List<String> seen = new ArrayList<>();
        bus.subscribe("echo", OrderPlaced.class,
            e -> bus.publish(StockReserved.of(e.orderId(), 1, 1, 100, e.customerId())));
        bus.subscribe("audit", Event.class, e -> seen.add(e.getClass().getSimpleName()));

        bus.publish(OrderPlaced.of(1, 1, 1, "C-001"));
        bus.processAll();

        assertThat(seen).containsExactly("OrderPlaced", "StockReserved");
        assertThat(bus.pendingCount()).isEqualTo(0);
    }
}
