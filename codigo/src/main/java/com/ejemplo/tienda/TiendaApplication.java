package com.ejemplo.tienda;

import com.ejemplo.tienda.bus.EventBusException;
import com.ejemplo.tienda.bus.RabbitMqEventBus;
import com.ejemplo.tienda.event.Event;
import com.ejemplo.tienda.orders.Order;
import com.ejemplo.tienda.orders.OrderStatus;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Demo over the real broker (RabbitMQ, docker compose up -d --wait). It runs the
 * Hoja 1 sequence and prints the final state of each order once the flow finished.
 * The graded samples (MuestraHoja1, MuestraHoja2) use the in-memory bus; see LEEME.md.
 */
public class TiendaApplication {

    public static void main(String[] args) throws InterruptedException {
        boolean keep = List.of(args).contains("--keep");
        String namespace = "tienda-" + UUID.randomUUID().toString().substring(0, 6);
        RabbitMqEventBus.Settings settings = RabbitMqEventBus.Settings.fromEnv();

        try (RabbitMqEventBus bus = new RabbitMqEventBus(settings, namespace)) {
            run(bus);
            if (keep) {
                System.out.println();
                System.out.println("Colas conservadas con el prefijo " + namespace
                    + " (abre http://localhost:15672, usuario tienda, clave tienda)");
            } else {
                bus.deleteAll();
            }
        } catch (EventBusException error) {
            System.err.println("No pude hablar con RabbitMQ en " + settings.address()
                + ". Levantalo con: docker compose up -d");
            System.err.println("Detalle: " + error.getMessage() + " / " + error.getCause());
            System.exit(1);
        }
    }

    private static void run(RabbitMqEventBus bus) throws InterruptedException {
        Tienda tienda = Tienda.montar(bus, System.out::println);
        bus.subscribe("audit", Event.class,
            event -> System.out.println("[evento] " + event.getClass().getSimpleName() + " " + event.orderId()));

        int u = Tienda.UMBRAL_U;
        int stock = Tienda.STOCK_INICIAL;
        List<Order> placed = List.of(
            tienda.orders().placeOrder("C-001", Tienda.PRODUCTO_ID, u - 1),
            tienda.orders().placeOrder("C-002", Tienda.PRODUCTO_ID, u),
            tienda.orders().placeOrder("C-001", Tienda.PRODUCTO_ID, stock - 2 * u + 2),
            tienda.orders().placeOrder("C-003", Tienda.PRODUCTO_ID, stock - 2 * u + 1));

        boolean finished = waitUntil(() -> placed.stream().allMatch(order ->
            tienda.orders().findById(order.id()).orElseThrow().estado() != OrderStatus.PENDIENTE),
            Duration.ofSeconds(10));
        if (!finished) {
            System.out.println("Pasaron 10 segundos y el flujo no termino. Revisa los consumidores.");
        }

        System.out.println();
        for (Order order : placed) {
            Order current = tienda.orders().findById(order.id()).orElseThrow();
            System.out.println("PEDIDO " + current.id() + " " + current.estado() + " total=" + current.total());
        }
        System.out.println("Stock de " + Tienda.PRODUCTO_NOMBRE + ": " + tienda.inventory().stockOf(Tienda.PRODUCTO_ID));
        System.out.println("Mensajes en la cola de errores: " + bus.deadLetterCount());
    }

    private static boolean waitUntil(BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condition.getAsBoolean();
    }
}
