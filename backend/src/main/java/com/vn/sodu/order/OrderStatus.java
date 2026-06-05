package com.vn.sodu.order;

public enum OrderStatus {
    NEW,
    WAITING_DEPOSIT,
    DEPOSIT_PAID,
    READY_FOR_FINAL_PAYMENT,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
