package com.vn.sodu.payment.service;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderReadyForSyncEvent;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.OrderSyncStatus;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PayOSCheckoutSession;
import com.vn.sodu.payment.PayOSGateway;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import com.vn.sodu.request.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter PAYMENT_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MOCK_PROVIDER = "PAYOS_MOCK";

    private final OrderPaymentRepository orderPaymentRepository;
    private final OrderRepository orderRepository;
    private final PayOSGateway payOSGateway;
    private final PaymentCalculationService paymentCalculationService;
    private final ApplicationEventPublisher eventPublisher;

    public void initializeOrderPaymentState(Order order) {
        if (order == null) {
            return;
        }
        order.setPaidAmount(paymentCalculationService.normalizeMoney(BigDecimal.ZERO));
        order.setRemainingAmount(paymentCalculationService.normalizeMoney(order.getTotalAmount()));
        order.setPaymentStatus(PaymentStatus.PENDING);
    }

    @Transactional
    public OrderPayment createPreorderFinalPayment(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order id is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getType() != OrderType.PREORDER) {
            throw new IllegalArgumentException("Only PREORDER orders support final-payment preparation");
        }
        if (order.getStatus() != OrderStatus.DEPOSIT_PAID) {
            throw new IllegalStateException("PREORDER final payment requires DEPOSIT_PAID status");
        }

        order.setStatus(OrderStatus.READY_FOR_FINAL_PAYMENT);
        orderRepository.save(order);
        return createPayment(order, PaymentType.FINAL);
    }

    @Transactional
    public OrderPayment createPayment(Order order, PaymentType type) {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("A persisted order is required to create a payment");
        }
        if (type == null) {
            throw new IllegalArgumentException("Payment type is required");
        }

        Order managedOrder = orderRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + order.getId()));
        List<OrderPayment> existingPayments = orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(managedOrder.getId());
        validatePaymentCreation(managedOrder, type, existingPayments);

        BigDecimal amount = paymentCalculationService.calculatePaymentAmount(managedOrder, type);
        if (amount.signum() <= 0) {
            throw new IllegalStateException("Calculated payment amount must be greater than 0");
        }

        OrderPayment payment = OrderPayment.builder()
                .order(managedOrder)
                .paymentCode(generateUniquePaymentCode())
                .type(type)
                .status(PaymentStatus.PENDING)
                .amount(amount)
                .provider(MOCK_PROVIDER)
                .build();

        PayOSCheckoutSession session = payOSGateway.createCheckout(managedOrder, payment);
        payment.setProviderReference(session.providerReference());
        payment.setCheckoutUrl(session.checkoutUrl());
        payment.setQrCode(session.qrCode());
        payment.setExpiresAt(session.expiresAt());

        OrderPayment savedPayment = orderPaymentRepository.save(payment);
        recalculateOrderPaymentState(managedOrder);
        return savedPayment;
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

        OrderPayment payment = orderPaymentRepository.findByPaymentCode(paymentCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentCode));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return payment;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setFailureReason(null);
        OrderPayment savedPayment = orderPaymentRepository.save(payment);

        Order updatedOrder = recalculateOrderPaymentState(payment.getOrder());
        updatedOrder = advanceOrderAfterPayment(updatedOrder, savedPayment);
        savedPayment.setOrder(updatedOrder);
        publishOrderReadyForSync(updatedOrder, savedPayment);
        return savedPayment;
    }

    private void publishOrderReadyForSync(Order order, OrderPayment payment) {
        if (order == null || order.getId() == null || payment == null) {
            return;
        }
        if (order.getSyncStatus() == OrderSyncStatus.SYNCED) {
            return;
        }
        if (payment.getType() == PaymentType.REFUND) {
            return;
        }
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            return;
        }
        if (!isEligibleForSyncEvent(order, payment)) {
            return;
        }
        eventPublisher.publishEvent(new OrderReadyForSyncEvent(order.getId(), payment.getPaymentCode()));
    }

    private void validatePaymentCreation(Order order, PaymentType type, List<OrderPayment> existingPayments) {
        if (type == PaymentType.REFUND) {
            throw new IllegalArgumentException("Refund payments cannot be created from the customer payment flow");
        }
        switch (order.getType()) {
            case NORMAL -> validateNormalOrderPaymentCreation(type, existingPayments);
            case PREORDER -> validatePreorderPaymentCreation(order, type, existingPayments);
            default -> throw new IllegalStateException("Payments are not enabled for order type " + order.getType());
        }
    }

    private void validateNormalOrderPaymentCreation(PaymentType type, List<OrderPayment> existingPayments) {
        if (type != PaymentType.FULL) {
            throw new IllegalArgumentException("NORMAL orders only support FULL payments");
        }
        if (hasBlockingPayment(existingPayments, PaymentType.FULL)) {
            throw new IllegalStateException("A FULL payment already exists for this order");
        }
    }

    private void validatePreorderPaymentCreation(Order order, PaymentType type, List<OrderPayment> existingPayments) {
        switch (type) {
            case DEPOSIT -> {
                if (order.getStatus() != OrderStatus.WAITING_DEPOSIT) {
                    throw new IllegalStateException("PREORDER deposit payments require WAITING_DEPOSIT status");
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

    private Order advanceOrderAfterPayment(Order order, OrderPayment payment) {
        if (order == null || payment == null) {
            return order;
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
            return orderRepository.save(order);
        }

        if (payment.getType() == PaymentType.FINAL
                && order.getStatus() == OrderStatus.READY_FOR_FINAL_PAYMENT
                && order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setStatus(OrderStatus.PROCESSING);
            return orderRepository.save(order);
        }

        return order;
    }

    private boolean isEligibleForSyncEvent(Order order, OrderPayment payment) {
        if (order.getType() == OrderType.NORMAL) {
            return true;
        }
        return order.getType() == OrderType.PREORDER
                && payment.getType() == PaymentType.FINAL
                && order.getStatus() == OrderStatus.PROCESSING;
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
