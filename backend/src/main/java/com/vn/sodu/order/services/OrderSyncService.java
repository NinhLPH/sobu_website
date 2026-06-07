package com.vn.sodu.order.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.dto.NhanhOrderAddRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.nhanh.dto.NhanhOrderEditRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderEditResult;
import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.order.NhanhSyncStage;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderSyncStatus;
import com.vn.sodu.order.nhanh.NhanhOrderGateway;
import com.vn.sodu.order.nhanh.NhanhSyncAttempt;
import com.vn.sodu.order.nhanh.NhanhSyncAttemptRepository;
import com.vn.sodu.order.nhanh.NhanhSyncAttemptStatus;
import com.vn.sodu.order.nhanh.NhanhSyncOperationType;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.repo.RequestRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSyncService {

    private static final int MAX_SYNC_ERROR_LENGTH = 1000;

    private final OrderRepository orderRepository;
    private final RequestRepo requestRepo;
    private final NhanhService nhanhService;
    private final NhanhOrderGateway nhanhOrderGateway;
    private final NhanhSyncAttemptRepository nhanhSyncAttemptRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public void syncOrderToNhanh(Long orderId) {
        syncOrderToNhanh(orderId, null);
    }

    public void syncOrderToNhanh(Long orderId, String paymentCode) {
        Order order = loadOrder(orderId);
        SyncTarget target = resolveSyncTarget(order, paymentCode);
        if (target == null) {
            log.info("Skipping Nhanh sync for order id={} because no eligible paid payment was found", orderId);
            return;
        }

        try {
            String accessToken = nhanhService.getValidAccessToken();
            if (target.operationType() == NhanhSyncOperationType.NHANH_EDIT_PREORDER_FINAL
                    && (order.getNhanhOrderId() == null || order.getNhanhOrderId().isBlank())) {
                ensurePreorderBaseOrderExists(order, accessToken);
                order = loadOrder(orderId);
            }

            switch (target.operationType()) {
                case NHANH_ADD_NORMAL, NHANH_ADD_PREORDER_DEPOSIT -> syncAdd(order, target, accessToken);
                case NHANH_EDIT_PREORDER_FINAL -> syncEdit(order, target, accessToken);
            }
        } catch (Exception ex) {
            transactionTemplate.executeWithoutResult(status -> markFailed(orderId, ex));
        }
    }

    public Order retryOrderSync(Long orderId) {
        syncOrderToNhanh(orderId, null);
        return loadOrder(orderId);
    }

    private void syncAdd(Order order, SyncTarget target, String accessToken) {
        NhanhOrderAddRequest request = nhanhOrderGateway.buildAddRequest(order, target.payment());
        String requestPayload = toJson(request);
        String fingerprint = sha256(requestPayload);
        String baseKey = buildBaseKey(target.operationType(), order.getId(), target.payment().getId());
        String idempotencyKey = buildIdempotencyKey(baseKey, fingerprint);
        NhanhSyncAttempt attempt = loadOrCreateAttempt(order, target, baseKey, idempotencyKey, fingerprint, requestPayload);

        if (attempt.getStatus() == NhanhSyncAttemptStatus.SUCCESS) {
            NhanhOrderAddResult existingResult = parseJson(attempt.getResponsePayload(), NhanhOrderAddResult.class);
            transactionTemplate.executeWithoutResult(status -> markAddSynced(order.getId(), target.stage(), existingResult));
            return;
        }

        try {
            NhanhOrderAddResult result = nhanhOrderGateway.createOrder(request, accessToken);
            String responsePayload = toJson(result);
            attempt.setStatus(NhanhSyncAttemptStatus.SUCCESS);
            attempt.setResponsePayload(responsePayload);
            attempt.setLastMessage(successMessage(target.operationType(), result));
            attempt.setCompletedAt(LocalDateTime.now());
            nhanhSyncAttemptRepository.save(attempt);
            NhanhOrderAddResult finalResult = result;
            transactionTemplate.executeWithoutResult(status -> markAddSynced(order.getId(), target.stage(), finalResult));
        } catch (Exception ex) {
            attempt.setStatus(NhanhSyncAttemptStatus.FAILED);
            attempt.setLastMessage(truncate(messageOf(ex)));
            attempt.setCompletedAt(LocalDateTime.now());
            nhanhSyncAttemptRepository.save(attempt);
            throw ex;
        }
    }

    private void syncEdit(Order order, SyncTarget target, String accessToken) {
        NhanhOrderEditRequest request = nhanhOrderGateway.buildEditRequest(order, target.payment());
        String requestPayload = toJson(request);
        String fingerprint = sha256(requestPayload);
        String baseKey = buildBaseKey(target.operationType(), order.getId(), target.payment().getId());
        String idempotencyKey = buildIdempotencyKey(baseKey, fingerprint);
        NhanhSyncAttempt attempt = loadOrCreateAttempt(order, target, baseKey, idempotencyKey, fingerprint, requestPayload);

        if (attempt.getStatus() == NhanhSyncAttemptStatus.SUCCESS) {
            transactionTemplate.executeWithoutResult(status -> markEditSynced(order.getId(), target.stage(), target.payment()));
            return;
        }

        try {
            NhanhOrderEditResult result = nhanhOrderGateway.editOrder(request, accessToken);
            attempt.setStatus(NhanhSyncAttemptStatus.SUCCESS);
            attempt.setResponsePayload(toJson(result));
            attempt.setLastMessage(successMessage(target.operationType(), null));
            attempt.setCompletedAt(LocalDateTime.now());
            nhanhSyncAttemptRepository.save(attempt);
            transactionTemplate.executeWithoutResult(status -> markEditSynced(order.getId(), target.stage(), target.payment()));
        } catch (Exception ex) {
            attempt.setStatus(NhanhSyncAttemptStatus.FAILED);
            attempt.setLastMessage(truncate(messageOf(ex)));
            attempt.setCompletedAt(LocalDateTime.now());
            nhanhSyncAttemptRepository.save(attempt);
            throw ex;
        }
    }

    private void ensurePreorderBaseOrderExists(Order order, String accessToken) {
        SyncTarget depositTarget = resolvePaidPayment(order, PaymentType.DEPOSIT)
                .map(payment -> new SyncTarget(payment, NhanhSyncOperationType.NHANH_ADD_PREORDER_DEPOSIT, NhanhSyncStage.PREORDER_DEPOSIT_CREATED))
                .orElseThrow(() -> new IllegalStateException("Cannot update preorder in Nhanh before a paid deposit exists"));
        syncAdd(order, depositTarget, accessToken);
    }

    private SyncTarget resolveSyncTarget(Order order, String paymentCode) {
        if (order == null) {
            return null;
        }
        Optional<OrderPayment> explicitPayment = findPayment(order, paymentCode);
        if (explicitPayment.isPresent()) {
            return toSyncTarget(order, explicitPayment.get());
        }

        if (order.getType() == OrderType.NORMAL) {
            return resolvePaidPayment(order, PaymentType.FULL)
                    .map(payment -> new SyncTarget(payment, NhanhSyncOperationType.NHANH_ADD_NORMAL, NhanhSyncStage.NORMAL_ORDER_CREATED))
                    .orElse(null);
        }
        if (order.getType() != OrderType.PREORDER) {
            return null;
        }

        Optional<OrderPayment> finalPayment = resolvePaidPayment(order, PaymentType.FINAL);
        if (finalPayment.isPresent()) {
            return new SyncTarget(finalPayment.get(), NhanhSyncOperationType.NHANH_EDIT_PREORDER_FINAL, NhanhSyncStage.PREORDER_FINAL_UPDATED);
        }
        return resolvePaidPayment(order, PaymentType.DEPOSIT)
                .map(payment -> new SyncTarget(payment, NhanhSyncOperationType.NHANH_ADD_PREORDER_DEPOSIT, NhanhSyncStage.PREORDER_DEPOSIT_CREATED))
                .orElse(null);
    }

    private SyncTarget toSyncTarget(Order order, OrderPayment payment) {
        if (payment == null || payment.getStatus() != PaymentStatus.PAID) {
            return null;
        }
        if (order.getType() == OrderType.NORMAL && payment.getType() == PaymentType.FULL) {
            return new SyncTarget(payment, NhanhSyncOperationType.NHANH_ADD_NORMAL, NhanhSyncStage.NORMAL_ORDER_CREATED);
        }
        if (order.getType() == OrderType.PREORDER && payment.getType() == PaymentType.DEPOSIT) {
            return new SyncTarget(payment, NhanhSyncOperationType.NHANH_ADD_PREORDER_DEPOSIT, NhanhSyncStage.PREORDER_DEPOSIT_CREATED);
        }
        if (order.getType() == OrderType.PREORDER && payment.getType() == PaymentType.FINAL) {
            return new SyncTarget(payment, NhanhSyncOperationType.NHANH_EDIT_PREORDER_FINAL, NhanhSyncStage.PREORDER_FINAL_UPDATED);
        }
        return null;
    }

    private Optional<OrderPayment> resolvePaidPayment(Order order, PaymentType type) {
        if (order.getPayments() == null) {
            return Optional.empty();
        }
        return order.getPayments().stream()
                .filter(payment -> payment != null
                        && payment.getType() == type
                        && payment.getStatus() == PaymentStatus.PAID)
                .max(Comparator.comparing(OrderPayment::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OrderPayment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private Optional<OrderPayment> findPayment(Order order, String paymentCode) {
        if (order == null || order.getPayments() == null || paymentCode == null || paymentCode.isBlank()) {
            return Optional.empty();
        }
        return order.getPayments().stream()
                .filter(payment -> payment != null && paymentCode.trim().equals(payment.getPaymentCode()))
                .findFirst();
    }

    private NhanhSyncAttempt loadOrCreateAttempt(
            Order order,
            SyncTarget target,
            String baseKey,
            String idempotencyKey,
            String fingerprint,
            String requestPayload
    ) {
        return nhanhSyncAttemptRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> nhanhSyncAttemptRepository.save(NhanhSyncAttempt.builder()
                        .order(order)
                        .payment(target.payment())
                        .operationType(target.operationType())
                        .baseKey(baseKey)
                        .idempotencyKey(idempotencyKey)
                        .requestFingerprint(fingerprint)
                        .requestPayload(requestPayload)
                        .status(NhanhSyncAttemptStatus.PENDING)
                        .build()));
    }

    private void markAddSynced(Long orderId, NhanhSyncStage stage, NhanhOrderAddResult result) {
        Order order = loadOrder(orderId);
        String resolvedNhanhOrderId = resolveNhanhOrderId(order, result);
        if (resolvedNhanhOrderId == null || resolvedNhanhOrderId.isBlank()) {
            throw new IllegalStateException("Nhanh add completed without a stored nhanhOrderId; manual reconciliation is required");
        }

        order.setNhanhOrderId(resolvedNhanhOrderId);
        order.setNhanhOrderCode(defaultText(order.getNhanhOrderCode(), order.getOrderCode()));
        order.setNhanhSyncStage(stage);
        order.setSyncStatus(OrderSyncStatus.SYNCED);
        order.setSyncError(result != null && result.isDuplicate()
                ? "Nhanh duplicate appOrderId; reused existing local Nhanh order reference."
                : null);
        order.setLastSyncAt(LocalDateTime.now());
        order.setLastSyncMessage(successMessage(
                stage == NhanhSyncStage.NORMAL_ORDER_CREATED
                        ? NhanhSyncOperationType.NHANH_ADD_NORMAL
                        : NhanhSyncOperationType.NHANH_ADD_PREORDER_DEPOSIT,
                result
        ));
        orderRepository.save(order);

        Request request = order.getRequest();
        if (request != null && request.getId() != null) {
            request.setNhanhOrderId(order.getNhanhOrderId());
            request.setNhanhOrderCode(order.getNhanhOrderCode());
            requestRepo.save(request);
        }
    }

    private void markEditSynced(Long orderId, NhanhSyncStage stage, OrderPayment payment) {
        Order order = loadOrder(orderId);
        order.setNhanhSyncStage(stage);
        order.setSyncStatus(OrderSyncStatus.SYNCED);
        order.setSyncError(null);
        order.setLastSyncAt(LocalDateTime.now());
        order.setLastSyncMessage("Nhanh preorder final payment updated successfully for payment " + payment.getPaymentCode());
        orderRepository.save(order);
    }

    private String resolveNhanhOrderId(Order order, NhanhOrderAddResult result) {
        if (result != null && result.getId() != null) {
            return result.getId().toString();
        }
        if (result != null && result.isDuplicate()) {
            return order.getNhanhOrderId();
        }
        return order.getNhanhOrderId();
    }

    private void markFailed(Long orderId, Exception ex) {
        Order order = loadOrder(orderId);
        String message = truncate(messageOf(ex));
        order.setSyncStatus(OrderSyncStatus.FAILED);
        order.setSyncError(message);
        order.setLastSyncAt(LocalDateTime.now());
        order.setLastSyncMessage(message);
        orderRepository.save(order);
        log.warn("Nhanh order sync failed for order id={}, code={}: {}", order.getId(), order.getOrderCode(), message);
    }

    private Order loadOrder(Long orderId) {
        return orderRepository.findWithItemsAndRequestById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private String buildBaseKey(NhanhSyncOperationType operationType, Long orderId, Long paymentId) {
        return switch (operationType) {
            case NHANH_ADD_NORMAL -> "nhanh:add:normal:" + orderId + ":" + paymentId;
            case NHANH_ADD_PREORDER_DEPOSIT -> "nhanh:add:preorder-deposit:" + orderId + ":" + paymentId;
            case NHANH_EDIT_PREORDER_FINAL -> "nhanh:edit:preorder-final:" + orderId + ":" + paymentId;
        };
    }

    private String buildIdempotencyKey(String baseKey, String fingerprint) {
        String suffix = fingerprint == null ? "unknown" : fingerprint.substring(0, Math.min(12, fingerprint.length()));
        return baseKey + ":" + suffix;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize sync payload", ex);
        }
    }

    private <T> T parseJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to deserialize stored sync payload", ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String truncate(String message) {
        if (message == null || message.length() <= MAX_SYNC_ERROR_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_SYNC_ERROR_LENGTH);
    }

    private String messageOf(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private String successMessage(NhanhSyncOperationType operationType, NhanhOrderAddResult result) {
        if (result != null && result.isDuplicate()) {
            return "Nhanh reported duplicate appOrderId and the existing local Nhanh order reference was reused.";
        }
        return switch (operationType) {
            case NHANH_ADD_NORMAL -> "Nhanh normal order created successfully.";
            case NHANH_ADD_PREORDER_DEPOSIT -> "Nhanh preorder deposit order created successfully.";
            case NHANH_EDIT_PREORDER_FINAL -> "Nhanh preorder final payment updated successfully.";
        };
    }

    private String defaultText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private record SyncTarget(OrderPayment payment, NhanhSyncOperationType operationType, NhanhSyncStage stage) {
    }
}
