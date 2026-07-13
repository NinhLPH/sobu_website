package com.vn.sodu.payment;

public interface PayOSWebhookConfirmationClient {

    void confirmWebhook(String webhookUrl, String clientId, String apiKey);
}
