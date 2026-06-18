package com.marketplace.order;

/** Simulated order lifecycle: no real payment, just the documented state machine. */
public enum OrderStatus {
    PENDING,    // created at checkout, awaiting (simulated) payment
    PAID,       // buyer paid
    SHIPPED,    // seller shipped
    DELIVERED   // buyer confirmed receipt — unlocks reviews
}
