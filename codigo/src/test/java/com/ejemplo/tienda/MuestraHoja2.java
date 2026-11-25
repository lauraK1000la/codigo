package com.ejemplo.tienda;

import com.ejemplo.tienda.orders.Order;
import com.ejemplo.tienda.support.InMemoryEventBus;

/**
 * Hoja 2 sample (Q1, new discount tier), from a clean start and in a separate run.
 * Same entry point as MuestraHoja1. Row 1: u = 5, so 3u = 15.
 */
public class MuestraHoja2 {

    public static void main(String[] args) {
        InMemoryEventBus bus = new InMemoryEventBus();
        Tienda tienda = Tienda.montar(bus, aviso -> { });
        int u = Tienda.UMBRAL_U;

        Order[] pedidos = {
            tienda.orders().placeOrder("C-001", Tienda.PRODUCTO_ID, 3 * u),
            tienda.orders().placeOrder("C-002", Tienda.PRODUCTO_ID, 3 * u - 1),
            tienda.orders().placeOrder("C-003", Tienda.PRODUCTO_ID, 1),
        };

        bus.processAll(); // eventual consistency: print once the processing finished

        for (Order pedido : pedidos) {
            Order fin = tienda.orders().findById(pedido.id()).orElseThrow();
            System.out.println("PEDIDO " + fin.id() + " " + fin.estado() + " total=" + fin.total());
        }
    }
}
