package com.ejemplo.tienda.orders;

import com.ejemplo.tienda.bus.EventBus;
import com.ejemplo.tienda.event.OrderPlaced;
import com.ejemplo.tienda.event.StockRejected;
import com.ejemplo.tienda.event.StockReserved;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongPredicate;

/**
 * Producer of {@link OrderPlaced}. It validates the request (RN-5), gives the order its
 * consecutive id, announces the fact and returns a PENDIENTE order right away.
 * It does not know who reacts, nor how the total or the discount are computed.
 * Handlers run on the bus' threads, so the state is kept in thread-safe collections.
 */
public class OrderService {

    private final EventBus bus;
    private final LongPredicate productExists;
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    /**
     * @param productExists answers "does this product exist?" synchronously (RN-5).
     *                      It is wired at startup, so orders does not import inventory.
     */
    public OrderService(EventBus bus, LongPredicate productExists) {
        this.bus = bus;
        this.productExists = productExists;
        bus.subscribe("orders", StockReserved.class,
            event -> orders.computeIfPresent(event.orderId(), (id, order) -> order.confirmed(event.total())));
        bus.subscribe("orders", StockRejected.class,
            event -> orders.computeIfPresent(event.orderId(), (id, order) -> order.rejected(event.reason())));
    }

    /**
     * RN-5: a quantity &lt;= 0 or an unknown product is a validation error. It throws,
     * is not registered and does not consume an id.
     */
    public synchronized Order placeOrder(String clienteId, long productoId, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }
        if (!productExists.test(productoId)) {
            throw new IllegalArgumentException("El producto " + productoId + " no existe");
        }
        long id = counter.incrementAndGet();
        Order order = new Order(id, clienteId, productoId, cantidad, OrderStatus.PENDIENTE, 0, null);
        orders.put(id, order);
        bus.publish(OrderPlaced.of(id, productoId, cantidad, clienteId));
        return order;
    }

    /** RN-4: an order by its id. */
    public Optional<Order> findById(long id) {
        return Optional.ofNullable(orders.get(id));
    }

    /** RN-4: the sum of the totals of the CONFIRMADO orders of a customer. */
    public long totalPurchasedBy(String clienteId) {
        return orders.values().stream()
            .filter(order -> order.clienteId().equals(clienteId))
            .filter(order -> order.estado() == OrderStatus.CONFIRMADO)
            .mapToLong(Order::total)
            .sum();
    }
}
