package com.ejemplo.tienda;

import com.ejemplo.tienda.billing.BillingService;
import com.ejemplo.tienda.bus.EventBus;
import com.ejemplo.tienda.inventory.DescuentoPorVolumen;
import com.ejemplo.tienda.inventory.InventoryService;
import com.ejemplo.tienda.notifications.NotificationService;
import com.ejemplo.tienda.orders.OrderService;
import java.util.function.Consumer;

/**
 * The wiring, by hand and without a framework. This is the only place that knows every
 * module; the modules only know the bus and the events.
 *
 * <p>Variant Q1 / row 1: one product, Mouse, productoId 1, $45.000, 40 units,
 * volume threshold u = 5, discount d = 10 %.
 */
public record Tienda(
    OrderService orders,
    InventoryService inventory,
    BillingService billing,
    NotificationService notifications
) {

    public static final long PRODUCTO_ID = 1;
    public static final String PRODUCTO_NOMBRE = "Mouse";
    public static final long PRECIO = 45_000;
    public static final int STOCK_INICIAL = 40;
    public static final int UMBRAL_U = 5;
    public static final int DESCUENTO_D = 10;

    /** Creates every module on this bus, with the product of row 1. */
    public static Tienda montar(EventBus bus, Consumer<String> avisos) {
        InventoryService inventory = new InventoryService(bus, new DescuentoPorVolumen(UMBRAL_U, DESCUENTO_D));
        inventory.addProduct(PRODUCTO_ID, PRODUCTO_NOMBRE, STOCK_INICIAL, PRECIO);
        OrderService orders = new OrderService(bus, inventory::exists);
        BillingService billing = new BillingService(bus);
        NotificationService notifications = new NotificationService(bus, avisos);
        return new Tienda(orders, inventory, billing, notifications);
    }
}
