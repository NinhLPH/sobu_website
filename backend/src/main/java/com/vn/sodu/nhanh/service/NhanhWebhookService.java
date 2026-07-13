package com.vn.sodu.nhanh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLog;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogRepository;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NhanhWebhookService {

    private final NhanhProperties nhanhProperties;
    private final ObjectMapper objectMapper;
    private final NhanhWebhookEventLogRepository webhookEventLogRepository;

    public void receive(String rawBody, HttpHeaders headers) {
        try {
            String authorization = headers == null ? null : headers.getFirst(HttpHeaders.AUTHORIZATION);
            boolean authorized = isAuthorized(authorization);
            if (!authorized) {
                log.warn("Rejected Nhanh webhook with invalid Authorization header");
                return;
            }

            JsonNode payload = parsePayload(rawBody);
            if (payload == null) {
                return;
            }

            String event = payload.get("event") == null ? null : payload.get("event").asText(null);
            String businessId = payload.get("businessId") == null ? null : payload.get("businessId").asText(null);
            Optional<NhanhWebhookEvent> supportedEvent = NhanhWebhookEvent.from(event);

            String payloadHash = sha256Hex(rawBody);
            String externalObjectId = extractExternalObjectId(payload);
            boolean authorizationPresent = authorization != null && !authorization.isBlank();

            NhanhWebhookEventLogStatus status;
            if (supportedEvent.isPresent()) {
                status = NhanhWebhookEventLogStatus.RECEIVED;
                log.info("Received Nhanh webhook event={}, type={}, businessId={}, payloadBytes={}",
                        event, supportedEvent.get(), businessId, rawBody == null ? 0 : rawBody.length());
            } else {
                status = NhanhWebhookEventLogStatus.IGNORED;
                log.warn("Received unsupported Nhanh webhook event={}, businessId={}, payloadBytes={}",
                        event, businessId, rawBody == null ? 0 : rawBody.length());
            }

            NhanhWebhookEventLog eventLog = NhanhWebhookEventLog.builder()
                    .eventName(event)
                    .eventType(supportedEvent.map(Enum::name).orElse(null))
                    .businessId(businessId)
                    .externalObjectId(externalObjectId)
                    .payloadHash(payloadHash)
                    .rawPayload(rawBody)
                    .authorizationPresent(authorizationPresent)
                    .status(status)
                    .receivedAt(LocalDateTime.now())
                    .build();

            webhookEventLogRepository.save(eventLog);
        } catch (Exception ex) {
            log.warn("Failed to record Nhanh webhook callback: {}", ex.getMessage(), ex);
        }
    }

    private String extractExternalObjectId(JsonNode payload) {
        JsonNode data = payload.get("data");
        if (data != null && data.isObject()) {
            JsonNode id = data.get("id");
            if (id != null && !id.isNull()) {
                return id.asText(null);
            }
        }
        return null;
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

    private String sha256Hex(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            log.warn("Failed to compute SHA-256 hash for Nhanh webhook payload", ex);
            return null;
        }
    }
}
