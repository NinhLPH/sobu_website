package com.vn.sodu.payment.service;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderReadyForSyncEvent;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.NhanhSyncStage;
import com.vn.sodu.order.OrderSyncStatus;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PayOSCheckoutSession;
import com.vn.sodu.payment.PayOSGateway;
import com.vn.sodu.payment.PayOSPaymentStatusSnapshot;
import com.vn.sodu.payment.PayOSProperties;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import com.vn.sodu.request.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter PAYMENT_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderPaymentRepository orderPaymentRepository;
    private final OrderRepository orderRepository;
    private final PayOSGateway payOSGateway;
    private final PayOSProperties payOSProperties;
    private final PaymentCalculationService paymentCalculationService;
    private final ApplicationEventPublisher eventPublisher;

    public void initializeOrderPaymentState(Order order) {
        if (order == null) {
            return;
        }
        order.setPaidAmount(paymentCalculationService.normalizeMoney(BigDecimal.ZERO));
        order.setRemainingAmount(paymentCalculationService.calculateOrderGrandTotal(order));
        order.setPaymentStatus(PaymentStatus.PENDING);
    }

    @Transactional(noRollbackFor = PaymentCheckoutCreationException.class)
    public OrderPayment createPreorderFinalPayment(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order id is required");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getType() != OrderType.PREORDER) {
            throw new IllegalArgumentException("Only PREORDER orders support final-payment preparation");
        }
        if (order.getStatus() != OrderStatus.DEPOSIT_PAID) {
            throw new IllegalStateException("PREORDER final payment requires DEPOSIT_PAID status");
        }

        return createPaymentInternal(order, PaymentType.FINAL, PaymentMethod.ONLINE);
    }

    @Transactional(noRollbackFor = PaymentCheckoutCreationException.class)
    public OrderPayment createPayment(Order order, PaymentType type) {
        return createPayment(order, type, PaymentMethod.ONLINE);
    }

    @Transactional(noRollbackFor = PaymentCheckoutCreationException.class)
    public OrderPayment createPayment(Order order, PaymentType type, PaymentMethod paymentMethod) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("A persisted order is required to create a payment");
        }
        if (type == null) {
            throw new IllegalArgumentException("Payment type is required");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is required");
        }

        Order managedOrder = loadOrderForPaymentCreation(order.getId(), type);
        return createPaymentInternal(managedOrder, type, paymentMethod);
    }

    private OrderPayment createPaymentInternal(Order managedOrder, PaymentType type, PaymentMethod paymentMethod) {
        OrderStatus originalOrderStatus = managedOrder.getStatus();
        boolean preparedPreorderFinalPayment = preparePreorderFinalPaymentIfNeeded(managedOrder, type, paymentMethod);
        List<OrderPayment> existingPayments = normalizePendingPayments(
                managedOrder.getId(),
                orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(managedOrder.getId())
        );
        validatePaymentCreation(managedOrder, type, paymentMethod, existingPayments);

        BigDecimal amount = paymentCalculationService.calculatePaymentAmount(managedOrder, type);
        if (amount.signum() <= 0) {
            throw new IllegalStateException("Calculated payment amount must be greater than 0");
        }

        OrderPayment payment = OrderPayment.builder()
                .order(managedOrder)
                .paymentCode(generateUniquePaymentCode())
                .type(type)
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .amount(amount)
                .provider(paymentMethod == PaymentMethod.COD ? "COD" : payOSGateway.providerName())
                .build();

        OrderPayment savedPayment = orderPaymentRepository.save(payment);
        if (savedPayment.getProviderOrderCode() == null) {
            savedPayment.setProviderOrderCode(savedPayment.getId());
            savedPayment = orderPaymentRepository.save(savedPayment);
        }

        if (paymentMethod == PaymentMethod.ONLINE) {
            try {
                PayOSCheckoutSession session = payOSGateway.createCheckout(managedOrder, savedPayment);
                savedPayment.setProviderReference(session.providerReference());
                savedPayment.setCheckoutUrl(session.checkoutUrl());
                savedPayment.setQrCode(session.qrCode());
                savedPayment.setExpiresAt(session.expiresAt());
                savedPayment = orderPaymentRepository.save(savedPayment);
            } catch (RuntimeException ex) {
                throw markPaymentFailedAfterCheckoutError(
                        managedOrder,
                        savedPayment,
                        originalOrderStatus,
                        preparedPreorderFinalPayment,
                        ex
                );
            }
        }

        Order updatedOrder = recalculateOrderPaymentState(managedOrder);
        if (paymentMethod == PaymentMethod.COD) {
            updatedOrder = advanceOrderAfterCodPayment(updatedOrder, savedPayment);
            savedPayment.setOrder(updatedOrder);
            publishOrderReadyForSync(updatedOrder, savedPayment);
        }
        return savedPayment;
    }

    private Order loadOrderForPaymentCreation(Long orderId, PaymentType type) {
        if (requiresOrderLock(type)) {
            return orderRepository.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    @Transactional
    public Order recalculateOrderPaymentState(Order order) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("A persisted order is required to recalculate payment state");
        }

        Order managedOrder = orderRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + order.getId()));
        List<OrderPayment> payments = orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(managedOrder.getId());
        paymentCalculationService.applyOrderPaymentState(managedOrder, payments);
        return orderRepository.save(managedOrder);
    }

    @Transactional
    public OrderPayment markPaymentPaid(String paymentCode) {
        if (paymentCode == null || paymentCode.isBlank()) {
            throw new IllegalArgumentException("Payment code is required");
        }

        OrderPayment payment = orderPaymentRepository.findByPaymentCodeForUpdate(paymentCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentCode));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return payment;
        }

        // Mark the payment as successfully paid and clear any previous errors
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setFailureReason(null);
        OrderPayment savedPayment = orderPaymentRepository.save(payment);

        // Recalculate order state, advance workflow if needed, and publish sync events if eligible
        Order updatedOrder = recalculateOrderPaymentState(payment.getOrder());
        updatedOrder = advanceOrderAfterPayment(updatedOrder, savedPayment);
        savedPayment.setOrder(updatedOrder);
        publishOrderReadyForSync(updatedOrder, savedPayment);
        return savedPayment;
    }

    @Transactional
    public OrderPayment markPaymentFailed(String paymentCode, String reason) {
        if (paymentCode == null || paymentCode.isBlank()) {
            throw new IllegalArgumentException("Payment code is required");
        }

        OrderPayment payment = orderPaymentRepository.findByPaymentCodeForUpdate(paymentCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentCode));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return payment;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason == null || reason.isBlank() ? "Payment failed" : reason);
        OrderPayment savedPayment = orderPaymentRepository.save(payment);
        Order updatedOrder = recalculateOrderPaymentState(payment.getOrder());
        savedPayment.setOrder(updatedOrder);
        return savedPayment;
    }

    @Transactional
    public OrderPayment markPaymentExpired(String paymentCode, String reason) {
        if (paymentCode == null || paymentCode.isBlank()) {
            throw new IllegalArgumentException("Payment code is required");
        }

        OrderPayment payment = orderPaymentRepository.findByPaymentCodeForUpdate(paymentCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentCode));

        if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.EXPIRED) {
            return payment;
        }

        payment.setStatus(PaymentStatus.EXPIRED);
        payment.setFailureReason(reason == null || reason.isBlank() ? "Payment session expired" : reason);
        OrderPayment savedPayment = orderPaymentRepository.save(payment);
        Order updatedOrder = recalculateOrderPaymentState(payment.getOrder());
        savedPayment.setOrder(updatedOrder);
        return savedPayment;
    }

    @Scheduled(
            initialDelayString = "#{@payOSProperties.reconciliation.initialDelayMs}",
            fixedDelayString = "#{@payOSProperties.reconciliation.fixedDelayMs}"
    )
    public void reconcilePendingOnlinePayments() {
        if (!payOSProperties.getReconciliation().isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleCutoff = now.minusSeconds(Math.max(0L, payOSProperties.getReconciliation().getStaleAfterSeconds()));
        int batchSize = Math.max(1, payOSProperties.getReconciliation().getBatchSize());
        List<OrderPayment> candidates = orderPaymentRepository.findByStatusInAndPaymentMethodOrderByCreatedAtAsc(
                EnumSet.of(PaymentStatus.PENDING, PaymentStatus.FAILED),
                PaymentMethod.ONLINE,
                PageRequest.of(0, batchSize)
        );

        for (OrderPayment payment : candidates) {
            if (payment == null || payment.getProviderOrderCode() == null) {
                continue;
            }
            if (!isReconciliationCandidate(payment, staleCutoff, now)) {
                continue;
            }
            try {
                reconcilePayment(payment, now);
            } catch (RuntimeException ignored) {
                // Transient provider lookup issues should not mutate local state.
            }
        }
    }

    @Transactional
    public List<OrderPayment> refreshPaymentStatuses(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order id is required");
        }
        List<OrderPayment> payments = normalizePendingPayments(
                orderId,
                orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(orderId)
        );
        Order order = payments.stream()
                .map(OrderPayment::getOrder)
                .filter(existingOrder -> existingOrder != null && existingOrder.getId() != null)
                .findFirst()
                .orElseGet(() -> orderRepository.findById(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId)));
        recalculateOrderPaymentState(order);
        return payments;
    }

    private void publishOrderReadyForSync(Order order, OrderPayment payment) {
        if (order == null || order.getId() == null || payment == null) {
            return;
        }
        if (payment.getType() == PaymentType.REFUND) {
            return;
        }
        if (!isEligibleForSyncEvent(order, payment)) {
            return;
        }
        eventPublisher.publishEvent(new OrderReadyForSyncEvent(order.getId(), payment.getPaymentCode()));
    }

    private void validatePaymentCreation(Order order, PaymentType type, PaymentMethod paymentMethod, List<OrderPayment> existingPayments) {
        if (type == PaymentType.REFUND) {
            throw new IllegalArgumentException("Refund payments cannot be created from the customer payment flow");
        }
        switch (order.getType()) {
            case NORMAL -> validateNormalOrderPaymentCreation(type, paymentMethod, existingPayments);
            case PREORDER -> validatePreorderPaymentCreation(order, type, paymentMethod, existingPayments);
            default -> throw new IllegalStateException("Payments are not enabled for order type " + order.getType());
        }
    }

    private boolean preparePreorderFinalPaymentIfNeeded(Order order, PaymentType type, PaymentMethod paymentMethod) {
        if (order == null
                || order.getType() != OrderType.PREORDER
                || type != PaymentType.FINAL
                || paymentMethod != PaymentMethod.ONLINE) {
            return false;
        }
        if (order.getStatus() == OrderStatus.DEPOSIT_PAID) {
            order.setStatus(OrderStatus.READY_FOR_FINAL_PAYMENT);
            return true;
        }
        return false;
    }

    private void validateNormalOrderPaymentCreation(PaymentType type, PaymentMethod paymentMethod, List<OrderPayment> existingPayments) {
        if (type != PaymentType.FULL) {
            throw new IllegalArgumentException("NORMAL orders only support FULL payments");
        }
        if (paymentMethod != PaymentMethod.ONLINE && paymentMethod != PaymentMethod.COD) {
            throw new IllegalArgumentException("NORMAL orders only support ONLINE or COD payment methods");
        }
        if (hasBlockingPayment(existingPayments, PaymentType.FULL)) {
            throw new IllegalStateException("A FULL payment already exists for this order");
        }
    }

    private void validatePreorderPaymentCreation(Order order, PaymentType type, PaymentMethod paymentMethod, List<OrderPayment> existingPayments) {
        switch (type) {
            case DEPOSIT -> {
                if (order.getStatus() != OrderStatus.WAITING_DEPOSIT) {
                    throw new IllegalStateException("PREORDER deposit payments require WAITING_DEPOSIT status");
                }
                if (paymentMethod != PaymentMethod.ONLINE) {
                    throw new IllegalArgumentException("PREORDER deposit payments only support ONLINE");
                }
                if (paymentCalculationService.normalizeMoney(order.getDepositAmount()).compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException("PREORDER deposit amount must be greater than 0");
                }
                if (hasBlockingPayment(existingPayments, PaymentType.DEPOSIT)) {
                    throw new IllegalStateException("A DEPOSIT payment already exists for this order");
                }
            }
            case FINAL -> {
                if (order.getStatus() != OrderStatus.READY_FOR_FINAL_PAYMENT) {
                    throw new IllegalStateException("PREORDER final payments require READY_FOR_FINAL_PAYMENT status");
                }
                if (paymentMethod != PaymentMethod.ONLINE && paymentMethod != PaymentMethod.COD) {
                    throw new IllegalArgumentException("PREORDER final payments only support ONLINE or COD");
                }
                if (paymentCalculationService.normalizeMoney(order.getRemainingAmount()).compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException("PREORDER remaining amount must be greater than 0");
                }
                if (hasBlockingPayment(existingPayments, PaymentType.FINAL)) {
                    throw new IllegalStateException("A FINAL payment already exists for this order");
                }
            }
            default -> throw new IllegalArgumentException("PREORDER orders do not support payment type " + type + " in Phase 4");
        }
    }

    private boolean hasBlockingPayment(List<OrderPayment> payments, PaymentType type) {
        if (payments == null) {
            return false;
        }
        return payments.stream().anyMatch(payment ->
                payment != null
                        && payment.getType() == type
                        && (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.PAID)
        );
    }

    private List<OrderPayment> normalizePendingPayments(Long orderId, List<OrderPayment> payments) {
        if (payments == null || payments.isEmpty()) {
            return payments == null ? List.of() : payments;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        List<OrderPayment> normalized = new ArrayList<>(payments);
        for (OrderPayment payment : normalized) {
            if (payment == null || payment.getStatus() != PaymentStatus.PENDING || payment.getExpiresAt() == null) {
                continue;
            }
            if (payment.getExpiresAt().isAfter(now)) {
                continue;
            }
            payment.setStatus(PaymentStatus.EXPIRED);
            if (payment.getFailureReason() == null || payment.getFailureReason().isBlank()) {
                payment.setFailureReason("Payment session expired");
            }
            orderPaymentRepository.save(payment);
            changed = true;
        }

        return changed
                ? orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(orderId)
                : normalized;
    }

    private Order advanceOrderAfterPayment(Order order, OrderPayment payment) {
        if (order == null || payment == null) {
            return order;
        }
        if (order.getType() == OrderType.NORMAL
                && payment.getType() == PaymentType.FULL
                && order.getPaymentStatus() == PaymentStatus.PAID
                && order.getStatus() != OrderStatus.PROCESSING) {
            order.setStatus(OrderStatus.PROCESSING);
            return orderRepository.save(order);
        }
        if (order.getType() != OrderType.PREORDER) {
            return order;
        }

        if (payment.getType() == PaymentType.DEPOSIT) {
            BigDecimal requiredDeposit = paymentCalculationService.normalizeMoney(order.getDepositAmount());
            if (requiredDeposit.compareTo(BigDecimal.ZERO) <= 0) {
                return order;
            }
            if (order.getStatus() != OrderStatus.WAITING_DEPOSIT) {
                return order;
            }
            if (paymentCalculationService.normalizeMoney(order.getPaidAmount()).compareTo(requiredDeposit) < 0) {
                return order;
            }
            order.setStatus(OrderStatus.DEPOSIT_PAID);
            order.setSyncStatus(OrderSyncStatus.PENDING);
            if (order.getNhanhSyncStage() == null) {
                order.setNhanhSyncStage(NhanhSyncStage.NONE);
            }
            return orderRepository.save(order);
        }

        if (payment.getType() == PaymentType.FINAL
                && order.getStatus() == OrderStatus.READY_FOR_FINAL_PAYMENT
                && order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setStatus(OrderStatus.PROCESSING);
            if (order.getNhanhSyncStage() == NhanhSyncStage.PREORDER_DEPOSIT_CREATED) {
                order.setSyncStatus(OrderSyncStatus.PENDING);
            }
            return orderRepository.save(order);
        }

        return order;
    }

    private Order advanceOrderAfterCodPayment(Order order, OrderPayment payment) {
        if (order == null || payment == null || payment.getPaymentMethod() != PaymentMethod.COD) {
            return order;
        }
        if (order.getType() != OrderType.PREORDER || payment.getType() != PaymentType.FINAL) {
            return order;
        }
        if (order.getStatus() != OrderStatus.READY_FOR_FINAL_PAYMENT) {
            return order;
        }
        order.setStatus(OrderStatus.PROCESSING);
        if (order.getNhanhSyncStage() == NhanhSyncStage.PREORDER_DEPOSIT_CREATED) {
            order.setSyncStatus(OrderSyncStatus.PENDING);
        }
        return orderRepository.save(order);
    }

    private boolean isEligibleForSyncEvent(Order order, OrderPayment payment) {
        if (payment != null && payment.getPaymentMethod() == PaymentMethod.COD) {
            if (order.getType() == OrderType.NORMAL) {
                return payment.getType() == PaymentType.FULL;
            }
            if (order.getType() == OrderType.PREORDER) {
                return payment.getType() == PaymentType.FINAL
                        && (order.getStatus() == OrderStatus.READY_FOR_FINAL_PAYMENT
                        || order.getStatus() == OrderStatus.PROCESSING);
            }
        }
        if (order.getType() == OrderType.NORMAL) {
            return payment.getType() == PaymentType.FULL
                    && order.getPaymentStatus() == PaymentStatus.PAID;
        }
        if (order.getType() != OrderType.PREORDER) {
            return false;
        }
        if (payment.getType() == PaymentType.DEPOSIT) {
            BigDecimal requiredDeposit = paymentCalculationService.normalizeMoney(order.getDepositAmount());
            return order.getStatus() == OrderStatus.DEPOSIT_PAID
                    && paymentCalculationService.normalizeMoney(order.getPaidAmount()).compareTo(requiredDeposit) >= 0;
        }
        return payment.getType() == PaymentType.FINAL
                && order.getStatus() == OrderStatus.PROCESSING
                && order.getPaymentStatus() == PaymentStatus.PAID;
    }

    private PaymentCheckoutCreationException markPaymentFailedAfterCheckoutError(
            Order managedOrder,
            OrderPayment payment,
            OrderStatus originalOrderStatus,
            boolean revertPreparedPreorderFinalPayment,
            RuntimeException ex
    ) {
        String failureReason = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Payment checkout creation failed"
                : ex.getMessage();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        orderPaymentRepository.save(payment);
        if (revertPreparedPreorderFinalPayment) {
            managedOrder.setStatus(originalOrderStatus);
        }
        recalculateOrderPaymentState(managedOrder);
        return new PaymentCheckoutCreationException(failureReason, ex);
    }

    private boolean requiresOrderLock(PaymentType type) {
        return type == PaymentType.FINAL;
    }

    private boolean isReconciliationCandidate(OrderPayment payment, LocalDateTime staleCutoff, LocalDateTime now) {
        if (payment.getCreatedAt() != null && payment.getCreatedAt().isAfter(staleCutoff)) {
            return payment.getExpiresAt() != null && !payment.getExpiresAt().isAfter(now);
        }
        return true;
    }

    private void reconcilePayment(OrderPayment payment, LocalDateTime now) {
        PayOSPaymentStatusSnapshot snapshot = payOSGateway.getPaymentStatus(payment.getProviderOrderCode());
        if (snapshot == null) {
            if (payment.getExpiresAt() != null && !payment.getExpiresAt().isAfter(now)) {
                markPaymentExpired(payment.getPaymentCode(), "Payment session expired");
            }
            return;
        }

        if (snapshot.status() == PaymentStatus.EXPIRED) {
            markPaymentExpired(payment.getPaymentCode(), "Payment session expired");
        }
    }

    private String generateUniquePaymentCode() {
        for (int i = 0; i < 20; i++) {
            String code = "SOBU-PAY-" + LocalDateTime.now().format(PAYMENT_CODE_FORMATTER) + "-" + String.format("%04d", RANDOM.nextInt(10_000));
            if (orderPaymentRepository.findByPaymentCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate unique payment code");
    }
}
