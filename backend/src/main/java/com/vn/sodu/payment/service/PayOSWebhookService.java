package com.vn.sodu.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentWebhookEvent;
import com.vn.sodu.payment.PaymentWebhookStatus;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import com.vn.sodu.payment.repo.PaymentWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSWebhookService {

    private static final String PROVIDER = "PAYOS";

    private final ObjectMapper objectMapper;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final PaymentService paymentService;

    public void receive(String rawBody, HttpHeaders headers) {
        PaymentWebhookEvent event = paymentWebhookEventRepository.save(PaymentWebhookEvent.builder()
                .provider(PROVIDER)
                .signature(extractSignature(headers))
                .rawPayload(rawBody)
                .status(PaymentWebhookStatus.RECEIVED)
                .build());

        try {
            JsonNode payload = parsePayload(rawBody);
            if (payload == null) {
                finalizeEvent(event, PaymentWebhookStatus.INVALID, Boolean.FALSE, "Payload is empty or not valid JSON");
                return;
            }

            enrichEvent(event, payload);
            boolean success = isPaymentSuccess(payload);
            event.setSuccess(success);
            if (!success) {
                finalizeEvent(event, PaymentWebhookStatus.IGNORED, Boolean.FALSE, "Webhook did not indicate a successful payment");
                return;
            }

            Optional<OrderPayment> payment = resolvePayment(event);
            if (payment.isEmpty()) {
                finalizeEvent(event, PaymentWebhookStatus.FAILED, Boolean.TRUE, "Order payment not found");
                return;
            }

            OrderPayment resolvedPayment = payment.get();
            event.setPaymentCode(resolvedPayment.getPaymentCode());
            event.setProviderReference(resolvedPayment.getProviderReference());
            if (resolvedPayment.getStatus() == PaymentStatus.PAID) {
                finalizeEvent(event, PaymentWebhookStatus.DUPLICATE, Boolean.TRUE, "Payment already marked as paid");
                return;
            }

            paymentService.markPaymentPaid(resolvedPayment.getPaymentCode());
            finalizeEvent(event, PaymentWebhookStatus.PROCESSED, Boolean.TRUE, "Payment confirmed");
        } catch (Exception ex) {
            log.warn("Failed to process PayOS webhook: {}", ex.getMessage(), ex);
            finalizeEvent(event, PaymentWebhookStatus.FAILED, event.getSuccess(), ex.getMessage());
        }
    }

    private void enrichEvent(PaymentWebhookEvent event, JsonNode payload) {
        event.setEventType(firstText(payload, "event", "type", "name"));
        event.setPaymentCode(firstText(payload, "paymentCode", "payment_code"));
        event.setProviderReference(firstText(payload, "providerReference", "provider_reference", "reference"));
    }

    private Optional<OrderPayment> resolvePayment(PaymentWebhookEvent event) {
        if (event.getPaymentCode() != null && !event.getPaymentCode().isBlank()) {
            return orderPaymentRepository.findByPaymentCode(event.getPaymentCode().trim());
        }
        if (event.getProviderReference() != null && !event.getProviderReference().isBlank()) {
            return orderPaymentRepository.findByProviderReference(event.getProviderReference().trim());
        }
        return Optional.empty();
    }

    private void finalizeEvent(PaymentWebhookEvent event, PaymentWebhookStatus status, Boolean success, String note) {
        event.setStatus(status);
        event.setSuccess(success);
        event.setProcessingNote(note);
        event.setProcessedAt(LocalDateTime.now());
        paymentWebhookEventRepository.save(event);
    }

    private JsonNode parsePayload(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isPaymentSuccess(JsonNode payload) {
        JsonNode successNode = firstNode(payload, "success", "paid");
        if (successNode != null && successNode.isBoolean()) {
            return successNode.asBoolean();
        }
        String status = firstText(payload, "status", "paymentStatus");
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return normalized.equals("PAID")
                || normalized.equals("SUCCESS")
                || normalized.equals("SUCCEEDED")
                || normalized.equals("COMPLETED");
    }

    private JsonNode firstNode(JsonNode payload, String... fieldNames) {
        if (payload == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode node = payload.get(fieldName);
            if (node != null && !node.isNull()) {
                return node;
            }
        }

        JsonNode data = payload.get("data");
        if (data == null || data.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode node = data.get(fieldName);
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private String firstText(JsonNode payload, String... fieldNames) {
        JsonNode node = firstNode(payload, fieldNames);
        return node == null ? null : node.asText(null);
    }

    private String extractSignature(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String signature = headers.getFirst("x-payos-signature");
        if (signature == null || signature.isBlank()) {
            signature = headers.getFirst("X-PAYOS-SIGNATURE");
        }
        return signature == null || signature.isBlank() ? null : signature.trim();
    }
}
