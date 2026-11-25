package com.ejemplo.tienda.inventory;

import com.ejemplo.tienda.bus.EventBus;
import com.ejemplo.tienda.event.Event;
import com.ejemplo.tienda.event.OrderPlaced;
import com.ejemplo.tienda.event.StockRejected;
import com.ejemplo.tienda.event.StockReserved;
import java.util.HashMap;
import java.util.Map;

/**
 * Reacts to {@link OrderPlaced}: keeps the units if there is stock (RN-3) and answers
 * with {@link StockReserved} (with the total, discount included) or {@link StockRejected}.
 * It knows the price, so it computes the total, but the discount rule itself lives in
 * {@link DescuentoPorVolumen}. Stock is never replenished in this exercise.
 */
public class InventoryService {

    private static final class Item {
        private final String name;
        private final long price;
        private int units;

        private Item(String name, int units, long price) {
            this.name = name;
            this.units = units;
            this.price = price;
        }
    }

    private final EventBus bus;
    private final DescuentoPorVolumen discount;
    private final Map<Long, Item> items = new HashMap<>();

    public InventoryService(EventBus bus, DescuentoPorVolumen discount) {
        this.bus = bus;
        this.discount = discount;
        bus.subscribe("inventory", OrderPlaced.class, this::onOrderPlaced);
    }

    public synchronized void addProduct(long productId, String name, int units, long price) {
        items.put(productId, new Item(name, units, price));
    }

    public synchronized boolean exists(long productId) {
        return items.containsKey(productId);
    }

    public synchronized int stockOf(long productId) {
        Item item = items.get(productId);
        return item == null ? 0 : item.units;
    }

    private void onOrderPlaced(OrderPlaced event) {
        // reserve() decides and updates the stock under the lock; the lock is released
        // before publishing, because publishing waits for the broker
        bus.publish(reserve(event));
    }

    private synchronized Event reserve(OrderPlaced event) {
        Item item = items.get(event.productId());
        if (item == null) {
            return StockRejected.of(event.orderId(), event.customerId(), "Producto desconocido");
        }
        // RN-3: an order that would go over the stock is rejected and consumes nothing;
        // an order that leaves the stock exactly at 0 is confirmed
        if (item.units < event.quantity()) {
            return StockRejected.of(event.orderId(), event.customerId(),
                "Stock insuficiente de " + item.name + ": quedan " + item.units);
        }
        item.units -= event.quantity();
        long total = discount.total(event.quantity(), item.price);
        return StockReserved.of(event.orderId(), event.productId(),
            event.quantity(), total, event.customerId());
    }
}
