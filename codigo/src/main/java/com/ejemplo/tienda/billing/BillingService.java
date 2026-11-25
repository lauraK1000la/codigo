package com.ejemplo.tienda.billing;

import com.ejemplo.tienda.bus.EventBus;
import com.ejemplo.tienda.event.StockReserved;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

/** Creates one invoice for each reserved order. */
public class BillingService {

    // Sorted by order id, thread-safe: the bus calls us from its own thread
    private final Map<Long, Invoice> invoices = new ConcurrentSkipListMap<>();

    public BillingService(EventBus bus) {
        bus.subscribe("billing", StockReserved.class, this::onStockReserved);
    }

    // Idempotent: brokers deliver "at least once", so the same event can arrive
    // twice. putIfAbsent makes the second delivery harmless.
    private void onStockReserved(StockReserved event) {
        invoices.putIfAbsent(event.orderId(),
            new Invoice(event.orderId(), event.customerId(), event.total()));
    }

    public Optional<Invoice> findByOrderId(long orderId) {
        return Optional.ofNullable(invoices.get(orderId));
    }

    public List<Invoice> all() {
        return List.copyOf(invoices.values());
    }
}
