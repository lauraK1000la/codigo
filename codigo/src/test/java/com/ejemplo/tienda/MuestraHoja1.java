package com.ejemplo.tienda;

import com.ejemplo.tienda.orders.Order;
import com.ejemplo.tienda.support.InMemoryEventBus;

/**
 * Hoja 1 sample. It goes through the real entry point of the architecture
 * (OrderService.placeOrder publishes OrderPlaced), lets the bus deliver every event
 * and then prints the final state of each order. Row 1: Mouse, stock 40, u = 5, d = 10 %.
 *
 * <p>It lives in src/test because it uses the in-memory bus (see ADR-0001).
 */
public class MuestraHoja1 {

    public static void main(String[] args) {
        InMemoryEventBus bus = new InMemoryEventBus();
        Tienda tienda = Tienda.montar(bus, aviso -> { });
        int u = Tienda.UMBRAL_U;
        int stock = Tienda.STOCK_INICIAL;

        Order[] pedidos = {
            tienda.orders().placeOrder("C-001", Tienda.PRODUCTO_ID, u - 1),
            tienda.orders().placeOrder("C-002", Tienda.PRODUCTO_ID, u),
            tienda.orders().placeOrder("C-001", Tienda.PRODUCTO_ID, stock - 2 * u + 2),
            tienda.orders().placeOrder("C-003", Tienda.PRODUCTO_ID, stock - 2 * u + 1),
        };

        bus.processAll(); // eventual consistency: print once the processing finished

        for (Order pedido : pedidos) {
            Order fin = tienda.orders().findById(pedido.id()).orElseThrow();
            System.out.println("PEDIDO " + fin.id() + " " + fin.estado() + " total=" + fin.total());
        }
    }
}
