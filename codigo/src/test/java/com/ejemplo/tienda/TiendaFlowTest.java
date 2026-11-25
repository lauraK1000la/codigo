package com.ejemplo.tienda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ejemplo.tienda.orders.Order;
import com.ejemplo.tienda.orders.OrderStatus;
import com.ejemplo.tienda.support.InMemoryEventBus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The business flow through its real entry point (OrderService.placeOrder) and the
 * bus, with the in-memory double. Row 1: Mouse, $45.000, 40 units, u = 5, d = 10 %.
 */
class TiendaFlowTest {

    private InMemoryEventBus bus;
    private Tienda tienda;

    @BeforeEach
    void setUp() {
        bus = new InMemoryEventBus();
        tienda = Tienda.montar(bus, message -> { });
    }

    private Order place(String cliente, int cantidad) {
        return tienda.orders().placeOrder(cliente, Tienda.PRODUCTO_ID, cantidad);
    }

    private Order current(Order order) {
        return tienda.orders().findById(order.id()).orElseThrow();
    }

    @Test
    void elPedidoQuedaPendienteHastaQueElBusEntregaLosEventos() {
        Order placed = place("C-001", 4);

        assertThat(placed.estado()).isEqualTo(OrderStatus.PENDIENTE);
        assertThat(current(placed).total()).isEqualTo(0L);

        bus.processAll();

        assertThat(current(placed).estado()).isEqualTo(OrderStatus.CONFIRMADO);
        assertThat(current(placed).total()).isEqualTo(180_000L);
    }

    @Test
    void rn3AnOrderThatWouldGoOverTheStockIsRejectedAndConsumesNothing() {
        // 4 + 5 confirmed leave 31 units; 32 goes over by 1; 31 leaves the stock at exactly 0
        List<Order> placed = List.of(place("C-001", 4), place("C-002", 5),
            place("C-001", 32), place("C-003", 31));
        bus.processAll();

        assertThat(placed.stream().map(this::current).map(Order::estado).toList())
            .containsExactly(OrderStatus.CONFIRMADO, OrderStatus.CONFIRMADO,
                OrderStatus.RECHAZADO, OrderStatus.CONFIRMADO);
        assertThat(current(placed.get(2)).total()).isEqualTo(0L);
        assertThat(tienda.inventory().stockOf(Tienda.PRODUCTO_ID)).isEqualTo(0);
    }

    @Test
    void rn3RejectedOrdersAlsoGetAnIdAndAreRegistered() {
        Order tooMany = place("C-001", 41);
        Order next = place("C-002", 1);
        bus.processAll();

        assertThat(tooMany.id()).isEqualTo(1L);
        assertThat(next.id()).isEqualTo(2L);
        assertThat(current(tooMany).estado()).isEqualTo(OrderStatus.RECHAZADO);
        assertThat(tienda.inventory().stockOf(Tienda.PRODUCTO_ID)).isEqualTo(39);
    }

    @Test
    void rn4TotalPurchasedByACustomerOnlyCountsConfirmedOrders() {
        place("C-001", 4);   // 180000, confirmed
        place("C-001", 99);  // rejected, total 0
        place("C-002", 5);   // another customer
        bus.processAll();

        assertThat(tienda.orders().totalPurchasedBy("C-001")).isEqualTo(180_000L);
        assertThat(tienda.orders().totalPurchasedBy("C-002")).isEqualTo(202_500L);
        assertThat(tienda.orders().findById(2)).isPresent();
    }

    @Test
    void rn5ValidationErrorsAreNotOrdersAndDoNotConsumeAnId() {
        assertThatThrownBy(() -> place("C-001", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> place("C-001", -3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tienda.orders().placeOrder("C-001", 99, 1))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(bus.pendingCount()).isEqualTo(0);
        assertThat(place("C-001", 1).id()).isEqualTo(1L);
    }

    @Test
    void aConfirmedOrderGetsOneInvoiceAndTheCustomerIsNotified() {
        place("C-002", 5);
        bus.processAll();

        assertThat(tienda.billing().all()).hasSize(1);
        assertThat(tienda.billing().findByOrderId(1).orElseThrow().total()).isEqualTo(202_500L);
        assertThat(tienda.notifications().sent()).hasSize(2);
    }
}
