package com.vn.sodu.order.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.dto.NhanhOrderAddRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.nhanh.dto.NhanhOrderEditRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderEditResult;
import com.vn.sodu.nhanh.dto.NhanhOrderListItem;
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
import com.vn.sodu.payment.repo.OrderPaymentRepository;
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
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSyncService {

    private static final int MAX_SYNC_ERROR_LENGTH = 1000;

    private final OrderRepository orderRepository;
    private final OrderPaymentRepository orderPaymentRepository;
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
            NhanhOrderAddResult reconciledResult = reconcileAddResult(order, existingResult, accessToken);
            updateAttemptPayloadIfChanged(attempt, existingResult, reconciledResult);
            transactionTemplate.executeWithoutResult(status -> markAddResult(order.getId(), target.stage(), reconciledResult));
            return;
        }

        try {
            NhanhOrderAddResult result = nhanhOrderGateway.createOrder(request, accessToken);
            NhanhOrderAddResult reconciledResult = reconcileAddResult(order, result, accessToken);
            String responsePayload = toJson(reconciledResult);
            attempt.setStatus(NhanhSyncAttemptStatus.SUCCESS);
            attempt.setResponsePayload(responsePayload);
            attempt.setLastMessage(successMessage(target.operationType(), reconciledResult));
            attempt.setCompletedAt(LocalDateTime.now());
            nhanhSyncAttemptRepository.save(attempt);
            NhanhOrderAddResult finalResult = reconciledResult;
            transactionTemplate.executeWithoutResult(status -> markAddResult(order.getId(), target.stage(), finalResult));
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
            return resolveSyncReadyPayment(order, PaymentType.FULL)
                    .map(payment -> new SyncTarget(payment, NhanhSyncOperationType.NHANH_ADD_NORMAL, NhanhSyncStage.NORMAL_ORDER_CREATED))
                    .orElse(null);
        }
        if (order.getType() != OrderType.PREORDER) {
            return null;
        }

        Optional<OrderPayment> finalPayment = resolveSyncReadyPayment(order, PaymentType.FINAL);
        if (finalPayment.isPresent()) {
            return new SyncTarget(finalPayment.get(), NhanhSyncOperationType.NHANH_EDIT_PREORDER_FINAL, NhanhSyncStage.PREORDER_FINAL_UPDATED);
        }
        return resolvePaidPayment(order, PaymentType.DEPOSIT)
                .map(payment -> new SyncTarget(payment, NhanhSyncOperationType.NHANH_ADD_PREORDER_DEPOSIT, NhanhSyncStage.PREORDER_DEPOSIT_CREATED))
                .orElse(null);
    }

    private SyncTarget toSyncTarget(Order order, OrderPayment payment) {
        if (!isSyncReadyPayment(order, payment)) {
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

    private Optional<OrderPayment> resolveSyncReadyPayment(Order order, PaymentType type) {
        if (order == null || order.getPayments() == null) {
            return Optional.empty();
        }
        return order.getPayments().stream()
                .filter(payment -> payment != null
                        && payment.getType() == type
                        && isSyncReadyPayment(order, payment))
                .max(Comparator.comparing(OrderPayment::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OrderPayment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private boolean isSyncReadyPayment(Order order, OrderPayment payment) {
        if (order == null || payment == null) {
            return false;
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            return true;
        }
        return payment.getPaymentMethod() == com.vn.sodu.payment.PaymentMethod.COD
                && payment.getStatus() == PaymentStatus.PENDING
                && ((order.getType() == OrderType.NORMAL && payment.getType() == PaymentType.FULL)
                || (order.getType() == OrderType.PREORDER && payment.getType() == PaymentType.FINAL));
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

    private void markAddResult(Long orderId, NhanhSyncStage stage, NhanhOrderAddResult result) {
        Order order = loadOrder(orderId);
        String resolvedNhanhOrderId = resolveNhanhOrderId(order, result);
        boolean needsReconcile = resolvedNhanhOrderId == null || resolvedNhanhOrderId.isBlank();

        order.setNhanhSyncStage(stage);
        order.setSyncStatus(needsReconcile ? OrderSyncStatus.NEED_RECONCILE : OrderSyncStatus.SYNCED);
        order.setNhanhOrderId(needsReconcile ? null : resolvedNhanhOrderId);
        order.setNhanhOrderCode(needsReconcile ? null : defaultText(order.getNhanhOrderCode(), order.getOrderCode()));

        String syncError = null;
        if (needsReconcile) {
            syncError = result != null && result.isDuplicate()
                    ? "Nhanh duplicate appOrderId detected but the existing Nhanh order ID could not be resolved automatically."
                    : "Nhanh order add completed without a resolvable Nhanh order ID; manual reconciliation is required.";
        } else if (result != null && result.isDuplicate()) {
            syncError = "Nhanh duplicate appOrderId detected and the existing Nhanh order ID was resolved automatically.";
        }
        order.setSyncError(syncError);
        order.setLastSyncAt(LocalDateTime.now());
        order.setLastSyncMessage(buildAddSyncMessage(
                needsReconcile,
                stage == NhanhSyncStage.NORMAL_ORDER_CREATED
                        ? NhanhSyncOperationType.NHANH_ADD_NORMAL
                        : NhanhSyncOperationType.NHANH_ADD_PREORDER_DEPOSIT,
                result
        ));
        log.info(
                "Persisting Nhanh add sync state: orderId={}, stage={}, duplicate={}, resolvedNhanhOrderId={}, syncStatus={}",
                orderId,
                stage,
                result != null && result.isDuplicate(),
                resolvedNhanhOrderId,
                order.getSyncStatus()
        );
        try {
            orderRepository.save(order);
            log.info(
                    "Persisted Nhanh add sync state: orderId={}, nhanhOrderId={}, syncStatus={}",
                    order.getId(),
                    order.getNhanhOrderId(),
                    order.getSyncStatus()
            );

            Request request = order.getRequest();
            if (!needsReconcile && request != null && request.getId() != null) {
                request.setNhanhOrderId(order.getNhanhOrderId());
                request.setNhanhOrderCode(order.getNhanhOrderCode());
                log.info(
                        "Persisting request Nhanh reference: requestId={}, orderId={}, nhanhOrderId={}, nhanhOrderCode={}",
                        request.getId(),
                        order.getId(),
                        request.getNhanhOrderId(),
                        request.getNhanhOrderCode()
                );
                requestRepo.save(request);
                log.info(
                        "Persisted request Nhanh reference: requestId={}, orderId={}",
                        request.getId(),
                        order.getId()
                );
            }
        } catch (RuntimeException ex) {
            log.error(
                    "Failed to persist Nhanh add sync state: orderId={}, resolvedNhanhOrderId={}, stage={}",
                    orderId,
                    resolvedNhanhOrderId,
                    stage,
                    ex
            );
            throw ex;
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
        if (result != null) {
            String resolvedFromResult = result.resolveNhanhOrderId();
            if (resolvedFromResult == null || resolvedFromResult.isBlank()) {
                log.warn(
                        "Nhanh order add parsed result has no resolvable nhanhOrderId: orderId={}, resultId={}, resultOrderId={}, trackingUrl={}, duplicate={}",
                        order.getId(),
                        result.getId(),
                        result.getOrderId(),
                        result.getTrackingUrl(),
                        result.isDuplicate()
                );
            } else {
                log.info(
                        "Nhanh order add parsed result resolved nhanhOrderId: orderId={}, resultId={}, resultOrderId={}, resolvedNhanhOrderId={}, duplicate={}",
                        order.getId(),
                        result.getId(),
                        result.getOrderId(),
                        resolvedFromResult,
                        result.isDuplicate()
                );
            }
            if (resolvedFromResult != null && !resolvedFromResult.isBlank()) {
                return resolvedFromResult;
            }
            String existing = normalizeRealNhanhOrderId(order.getNhanhOrderId());
            if (result.isDuplicate() && existing != null) {
                    log.warn(
                            "Nhanh duplicate result missing identifier, reusing existing local nhanhOrderId: orderId={}, existingNhanhOrderId={}",
                            order.getId(),
                            existing
                    );
                    return existing;
            }
        }
        return normalizeRealNhanhOrderId(order.getNhanhOrderId());
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
        return transactionTemplate.execute(status -> {
            Order order = orderRepository.findWithItemsAndRequestById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
            List<OrderPayment> payments = order.getPayments();
            if (payments != null) {
                payments.size();
                payments.sort(Comparator.comparing(
                        OrderPayment::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ));
            }
            return order;
        });
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

    private String buildAddSyncMessage(boolean needsReconcile, NhanhSyncOperationType operationType, NhanhOrderAddResult result) {
        if (needsReconcile) {
            return result != null && result.isDuplicate()
                    ? "Nhanh reported duplicate appOrderId but the existing Nhanh order ID could not be resolved automatically."
                    : "Nhanh order add response did not contain a resolvable Nhanh order ID; manual reconciliation is required.";
        }
        if (result != null && result.isDuplicate() && result.resolveNhanhOrderId() != null) {
            return "Nhanh reported duplicate appOrderId and the existing Nhanh order ID was resolved automatically.";
        }
        return switch (operationType) {
            case NHANH_ADD_NORMAL -> "Nhanh normal order created successfully.";
            case NHANH_ADD_PREORDER_DEPOSIT -> "Nhanh preorder deposit order created successfully.";
            case NHANH_EDIT_PREORDER_FINAL -> "Nhanh preorder final payment updated successfully.";
        };
    }

    private String successMessage(NhanhSyncOperationType operationType, NhanhOrderAddResult result) {
        if (operationType != NhanhSyncOperationType.NHANH_EDIT_PREORDER_FINAL) {
            return buildAddSyncMessage(false, operationType, result);
        }
        return switch (operationType) {
            case NHANH_EDIT_PREORDER_FINAL -> "Nhanh preorder final payment updated successfully.";
            case NHANH_ADD_NORMAL -> "Nhanh normal order created successfully.";
            case NHANH_ADD_PREORDER_DEPOSIT -> "Nhanh preorder deposit order created successfully.";
        };
    }

    private NhanhOrderAddResult reconcileAddResult(Order order, NhanhOrderAddResult result, String accessToken) {
        if (result == null) {
            return null;
        }
        if (result.resolveNhanhOrderId() != null) {
            return result;
        }

        String existingNhanhOrderId = normalizeRealNhanhOrderId(order.getNhanhOrderId());
        if (existingNhanhOrderId != null) {
            result.setOrderId(Long.valueOf(existingNhanhOrderId));
            log.info(
                    "Using existing numeric local nhanhOrderId for unresolved add result: orderId={}, nhanhOrderId={}",
                    order.getId(),
                    existingNhanhOrderId
            );
            return result;
        }

        Optional<NhanhOrderListItem> matchedOrder = nhanhOrderGateway.findOrderByReference(order, accessToken);
        if (matchedOrder.isPresent()) {
            String lookedUpNhanhOrderId = matchedOrder.get().resolveNhanhOrderId();
            if (lookedUpNhanhOrderId != null && !lookedUpNhanhOrderId.isBlank()) {
                result.setOrderId(Long.valueOf(lookedUpNhanhOrderId));
                log.info(
                        "Reconciled unresolved Nhanh add result via order lookup: orderId={}, lookedUpNhanhOrderId={}",
                        order.getId(),
                        lookedUpNhanhOrderId
                );
            }
        }
        return result;
    }

    private void updateAttemptPayloadIfChanged(
            NhanhSyncAttempt attempt,
            NhanhOrderAddResult previousResult,
            NhanhOrderAddResult reconciledResult
    ) {
        String previousPayload = toJson(previousResult);
        String updatedPayload = toJson(reconciledResult);
        if (Objects.equals(previousPayload, updatedPayload)) {
            return;
        }
        attempt.setResponsePayload(updatedPayload);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setLastMessage(successMessage(attempt.getOperationType(), reconciledResult));
        nhanhSyncAttemptRepository.save(attempt);
    }

    private String normalizeRealNhanhOrderId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || !trimmed.matches("\\d+")) {
            return null;
        }
        return trimmed;
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
