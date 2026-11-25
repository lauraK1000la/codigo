package com.ejemplo.tienda.notifications;

import com.ejemplo.tienda.bus.EventBus;
import com.ejemplo.tienda.event.OrderPlaced;
import com.ejemplo.tienda.event.StockRejected;
import com.ejemplo.tienda.event.StockReserved;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Tells the customer what happened. It listens to three events and publishes none. */
public class NotificationService {

    private final Consumer<String> out;
    private final List<String> sent = new CopyOnWriteArrayList<>();

    public NotificationService(EventBus bus, Consumer<String> out) {
        this.out = out;
        bus.subscribe("notifications", OrderPlaced.class, event -> send(event.customerId(),
            "Recibimos tu pedido " + event.orderId()));
        bus.subscribe("notifications", StockReserved.class, event -> send(event.customerId(),
            "Pedido " + event.orderId() + " confirmado. Total: " + event.total()));
        bus.subscribe("notifications", StockRejected.class, event -> send(event.customerId(),
            "No pudimos procesar el pedido " + event.orderId() + ": " + event.reason()));
    }

    public List<String> sent() {
        return List.copyOf(sent);
    }

    private void send(String customerId, String text) {
        String message = "[aviso a " + customerId + "] " + text;
        sent.add(message);
        out.accept(message);
    }
}
