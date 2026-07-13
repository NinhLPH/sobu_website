package com.vn.sodu.payment;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class PayOSWebhookConfirmationRestClient implements PayOSWebhookConfirmationClient {

    private static final String CONFIRM_WEBHOOK_URL = "https://api-merchant.payos.vn/confirm-webhook";

    private final RestClient restClient;

    public PayOSWebhookConfirmationRestClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public void confirmWebhook(String webhookUrl, String clientId, String apiKey) {
        ConfirmationResponse response = restClient.post()
                .uri(CONFIRM_WEBHOOK_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-client-id", clientId)
                .header("x-api-key", apiKey)
                .body(Map.of("webhookUrl", webhookUrl))
                .retrieve()
                .body(ConfirmationResponse.class);
        if (response == null || !"00".equals(response.code())) {
            throw new IllegalStateException("PayOS did not confirm the webhook URL");
        }
    }

    private record ConfirmationResponse(String code, String desc) {
    }
}
