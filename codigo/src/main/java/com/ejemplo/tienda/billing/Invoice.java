package com.ejemplo.tienda.billing;

public record Invoice(long orderId, String customerId, long total) {
}
