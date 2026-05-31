package com.vn.sodu.nhanh.webhook;

import java.util.Arrays;
import java.util.Optional;

public enum NhanhWebhookEvent {
    WEBHOOKS_ENABLED("webhooksEnabled"),
    PRODUCT_ADD("productAdd"),
    PRODUCT_UPDATE("productUpdate"),
    PRODUCT_DELETE("productDelete"),
    INVENTORY_CHANGE("inventoryChange"),
    ORDER_ADD("orderAdd"),
    ORDER_UPDATE("orderUpdate"),
    ORDER_DELETE("orderDelete"),
    ORDER_PARTIAL_RETURN("orderPartialReturn"),
    PAYMENT_RECEIVED("paymentReceived");

    private final String eventName;

    NhanhWebhookEvent(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }

    public static Optional<NhanhWebhookEvent> from(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(event -> event.eventName.equals(eventName))
                .findFirst();
    }
}
