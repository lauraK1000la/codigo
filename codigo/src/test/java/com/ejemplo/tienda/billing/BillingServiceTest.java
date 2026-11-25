package com.ejemplo.tienda.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejemplo.tienda.event.StockReserved;
import com.ejemplo.tienda.support.InMemoryEventBus;
import org.junit.jupiter.api.Test;

class BillingServiceTest {

    @Test
    void theSameEventDeliveredTwiceCreatesOneInvoice() {
        InMemoryEventBus bus = new InMemoryEventBus();
        BillingService billing = new BillingService(bus);
        StockReserved event = StockReserved.of(1, 1, 1, 45_000, "C-001");

        bus.publish(event);
        bus.publish(event);
        bus.processAll();

        assertThat(billing.all()).hasSize(1);
        assertThat(billing.findByOrderId(1)).isPresent();
    }
}
