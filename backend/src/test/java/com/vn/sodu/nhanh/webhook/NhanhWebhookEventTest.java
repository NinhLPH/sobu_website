package com.vn.sodu.nhanh.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NhanhWebhookEventTest {

    @Test
    void fromMapsNhanhEventNames() {
        assertThat(NhanhWebhookEvent.from("productAdd")).contains(NhanhWebhookEvent.PRODUCT_ADD);
        assertThat(NhanhWebhookEvent.from("productUpdate")).contains(NhanhWebhookEvent.PRODUCT_UPDATE);
        assertThat(NhanhWebhookEvent.from("productDelete")).contains(NhanhWebhookEvent.PRODUCT_DELETE);
        assertThat(NhanhWebhookEvent.from("inventoryChange")).contains(NhanhWebhookEvent.INVENTORY_CHANGE);
        assertThat(NhanhWebhookEvent.from("orderAdd")).contains(NhanhWebhookEvent.ORDER_ADD);
        assertThat(NhanhWebhookEvent.from("orderUpdate")).contains(NhanhWebhookEvent.ORDER_UPDATE);
        assertThat(NhanhWebhookEvent.from("orderDelete")).contains(NhanhWebhookEvent.ORDER_DELETE);
        assertThat(NhanhWebhookEvent.from("orderPartialReturn")).contains(NhanhWebhookEvent.ORDER_PARTIAL_RETURN);
        assertThat(NhanhWebhookEvent.from("paymentReceived")).contains(NhanhWebhookEvent.PAYMENT_RECEIVED);
    }
}
