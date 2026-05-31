package com.vn.sodu.nhanh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NhanhWebhookService {

    private final NhanhProperties nhanhProperties;
    private final ObjectMapper objectMapper;

    public void receive(String rawBody, HttpHeaders headers) {
        try {
            String authorization = headers == null ? null : headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (!isAuthorized(authorization)) {
                log.warn("Rejected Nhanh webhook with invalid Authorization header");
                return;
            }

            JsonNode payload = parsePayload(rawBody);
            String event = payload == null || payload.get("event") == null ? null : payload.get("event").asText(null);
            String businessId = payload == null || payload.get("businessId") == null ? null : payload.get("businessId").asText(null);
            Optional<NhanhWebhookEvent> supportedEvent = NhanhWebhookEvent.from(event);

            if (supportedEvent.isEmpty()) {
                log.warn("Received unsupported Nhanh webhook event={}, businessId={}, payloadBytes={}",
                        event,
                        businessId,
                        rawBody == null ? 0 : rawBody.length());
                return;
            }

            log.info("Received Nhanh webhook event={}, type={}, businessId={}, payloadBytes={}",
                    event,
                    supportedEvent.get(),
                    businessId,
                    rawBody == null ? 0 : rawBody.length());
        } catch (Exception ex) {
            log.warn("Failed to record Nhanh webhook callback: {}", ex.getMessage(), ex);
        }
    }

    private boolean isAuthorized(String authorization) {
        String expected = nhanhProperties.getWebhooks() == null
                ? null
                : nhanhProperties.getWebhooks().getVerifyToken();
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return expected.equals(authorization);
    }

    private JsonNode parsePayload(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            log.warn("Received Nhanh webhook with non-JSON body, payloadBytes={}", rawBody.length());
            return null;
        }
    }
}
