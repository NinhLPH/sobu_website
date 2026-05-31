package com.vn.sodu.nhanh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThatCode;

class NhanhWebhookServiceTest {

    @Test
    void receiveAcceptsValidAuthorizationHeader() {
        NhanhProperties properties = properties("verify-token");
        NhanhWebhookService service = new NhanhWebhookService(properties, new ObjectMapper());
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "verify-token");

        assertThatCode(() -> service.receive("{\"event\":\"webhooksEnabled\"}", headers))
                .doesNotThrowAnyException();
    }

    @Test
    void receiveDoesNotThrowWhenPayloadIsInvalidOrAuthorizationDoesNotMatch() {
        NhanhProperties properties = properties("verify-token");
        NhanhWebhookService service = new NhanhWebhookService(properties, new ObjectMapper());
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "wrong-token");

        assertThatCode(() -> service.receive("not-json", headers))
                .doesNotThrowAnyException();
    }

    @Test
    void receiveAcceptsConfiguredWebhookEvents() {
        NhanhProperties properties = properties("verify-token");
        NhanhWebhookService service = new NhanhWebhookService(properties, new ObjectMapper());
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "verify-token");

        for (NhanhWebhookEvent event : NhanhWebhookEvent.values()) {
            String payload = "{\"event\":\"" + event.eventName() + "\",\"businessId\":10000,\"data\":{}}";

            assertThatCode(() -> service.receive(payload, headers))
                    .doesNotThrowAnyException();
        }
    }

    private NhanhProperties properties(String verifyToken) {
        NhanhProperties properties = new NhanhProperties();
        NhanhProperties.Webhooks webhooks = new NhanhProperties.Webhooks();
        webhooks.setVerifyToken(verifyToken);
        properties.setWebhooks(webhooks);
        return properties;
    }
}
