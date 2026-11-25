package com.ejemplo.tienda.bus;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejemplo.tienda.event.OrderPlaced;
import com.ejemplo.tienda.event.StockReserved;
import com.ejemplo.tienda.support.Await;
import com.ejemplo.tienda.support.Broker;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The adapter against a real RabbitMQ. SKIPPED when the broker is not up. */
class RabbitMqEventBusTest {

    private RabbitMqEventBus bus;

    @BeforeEach
    void connect() {
        Broker.assumeRunning();
        bus = new RabbitMqEventBus(Broker.SETTINGS, "test-" + UUID.randomUUID().toString().substring(0, 6));
    }

    @AfterEach
    void cleanUp() {
        if (bus != null) {
            bus.deleteAll();
            bus.close();
        }
    }

    @Test
    void anEventTravelsAsJsonAndArrivesWithItsData() {
        List<StockReserved> received = new CopyOnWriteArrayList<>();
        bus.subscribe("billing", StockReserved.class, received::add);

        bus.publish(StockReserved.of(7, 1, 5, 202_500, "C-002"));

        Await.until("billing received the event", () -> received.size() == 1);
        assertThat(received.get(0).orderId()).isEqualTo(7L);
        assertThat(received.get(0).total()).isEqualTo(202_500L);
        assertThat(received.get(0).customerId()).isEqualTo("C-002");
    }

    @Test
    void eachConsumerNameGetsItsOwnCopy() {
        List<Long> a = new CopyOnWriteArrayList<>();
        List<Long> b = new CopyOnWriteArrayList<>();
        bus.subscribe("a", OrderPlaced.class, e -> a.add(e.orderId()));
        bus.subscribe("b", OrderPlaced.class, e -> b.add(e.orderId()));

        bus.publish(OrderPlaced.of(1, 1, 4, "C-001"));

        Await.until("both consumers received it", () -> a.size() == 1 && b.size() == 1);
        assertThat(a).containsExactly(1L);
        assertThat(b).containsExactly(1L);
    }

    @Test
    void aFailingHandlerSendsTheMessageToTheDeadLetterQueueAndOthersAreNotAffected() {
        List<Long> good = new CopyOnWriteArrayList<>();
        bus.subscribe("bad", OrderPlaced.class, e -> {
            throw new IllegalStateException("boom");
        });
        bus.subscribe("good", OrderPlaced.class, e -> good.add(e.orderId()));

        bus.publish(OrderPlaced.of(1, 1, 1, "C-001"));

        Await.until("good consumer received it", () -> good.size() == 1);
        Await.until("failed message reached the dead-letter queue", () -> bus.deadLetterCount() == 1);
        assertThat(good).containsExactly(1L);
    }
}
