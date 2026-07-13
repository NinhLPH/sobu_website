package com.vn.sodu.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vn.sodu.payment.PayOSProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Registers the merchant callback with PayOS. The provider keeps this setting
 * per payment channel, so setting PAYOS_WEBHOOK_URL alone is not sufficient.
 */
@Component
@RequiredArgsConstructor
public class PayOSWebhookRegistrationClient {

    private static final String CONFIRM_WEBHOOK_URL = "https://api-merchant.payos.vn/confirm-webhook";

    private final RestClient.Builder restClientBuilder;
    private final PayOSProperties payOSProperties;

    public void confirmWebhook(String webhookUrl) {
        requireText(payOSProperties.getClientId(), "PAYOS_CLIENT_ID");
        requireText(payOSProperties.getApiKey(), "PAYOS_API_KEY");

        JsonNode response = restClientBuilder.build()
                .post()
                .uri(CONFIRM_WEBHOOK_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-client-id", payOSProperties.getClientId().trim())
                .header("x-api-key", payOSProperties.getApiKey().trim())
                .body(Map.of("webhookUrl", webhookUrl))
                .retrieve()
                .body(JsonNode.class);

        String code = response == null ? null : response.path("code").asText(null);
        if (!"00".equals(code)) {
            String description = response == null ? null : response.path("desc").asText(null);
            throw new IllegalStateException(
                    "PayOS webhook confirmation failed" + (description == null || description.isBlank() ? "" : ": " + description)
            );
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required to confirm the PayOS webhook");
        }
    }
}
