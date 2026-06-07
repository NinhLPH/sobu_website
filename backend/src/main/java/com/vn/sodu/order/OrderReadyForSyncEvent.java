package com.vn.sodu.order;

public record OrderReadyForSyncEvent(Long orderId, String paymentCode) {
}
