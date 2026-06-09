package com.vn.sodu.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PayOSProperties;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentWebhookEvent;
import com.vn.sodu.payment.PaymentWebhookStatus;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import com.vn.sodu.payment.repo.PaymentWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private final PayOSProperties payOSProperties;

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
            if (event.getSignature() == null || event.getSignature().isBlank()) {
                event.setSignature(firstText(payload, "signature"));
            }
            if (!isSignatureValid(payload)) {
                finalizeEvent(event, PaymentWebhookStatus.INVALID, Boolean.FALSE, "Invalid PayOS webhook signature");
                return;
            }

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
        event.setEventType(firstText(payload, "event", "type", "name", "code"));
        event.setPaymentCode(firstText(payload, "paymentCode", "payment_code", "orderCode"));
        event.setProviderReference(firstText(payload, "providerReference", "provider_reference", "paymentLinkId", "reference"));
    }

    private Optional<OrderPayment> resolvePayment(PaymentWebhookEvent event) {
        Long providerOrderCode = parseLong(event.getPaymentCode());
        if (providerOrderCode != null) {
            Optional<OrderPayment> payment = orderPaymentRepository.findByProviderOrderCode(providerOrderCode);
            if (payment.isPresent()) {
                return payment;
            }
        }
        if (event.getPaymentCode() != null && !event.getPaymentCode().isBlank()) {
            Optional<OrderPayment> payment = orderPaymentRepository.findByPaymentCode(event.getPaymentCode().trim());
            if (payment.isPresent()) {
                return payment;
            }
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
                || normalized.equals("00")
                || normalized.equals("SUCCESS")
                || normalized.equals("SUCCEEDED")
                || normalized.equals("COMPLETED");
    }

    private boolean isSignatureValid(JsonNode payload) {
        if (!isRealMode()) {
            return true;
        }
        String signature = firstText(payload, "signature");
        JsonNode data = payload == null ? null : payload.get("data");
        if (signature == null || signature.isBlank() || data == null || !data.isObject()) {
            return false;
        }
        try {
            String signedData = toPayOSQueryString(data);
            String expected = hmacSha256Hex(signedData, payOSProperties.getChecksumKey());
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            log.warn("Failed to validate PayOS webhook signature: {}", ex.getMessage());
            return false;
        }
    }

    private String toPayOSQueryString(JsonNode data) {
        List<String> fieldNames = new ArrayList<>();
        data.fieldNames().forEachRemaining(fieldNames::add);
        fieldNames.sort(Comparator.naturalOrder());
        List<String> parts = new ArrayList<>();
        for (String fieldName : fieldNames) {
            JsonNode value = data.get(fieldName);
            parts.add(fieldName + "=" + toPayOSValue(value));
        }
        return String.join("&", parts);
    }

    private String toPayOSValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }
        if (value.isTextual()) {
            String text = value.asText();
            return "null".equals(text) || "undefined".equals(text) ? "" : text;
        }
        if (value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return value.asText("");
        }
    }

    private String hmacSha256Hex(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
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

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isRealMode() {
        return "real".equalsIgnoreCase(payOSProperties.getGatewayMode());
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
