package com.vn.sodu.payment.service;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.OrderReadyForSyncEvent;
import com.vn.sodu.order.OrderSyncStatus;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.payment.MockPayOSGateway;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PayOSCheckoutSession;
import com.vn.sodu.payment.PayOSGateway;
import com.vn.sodu.payment.PayOSProperties;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import com.vn.sodu.request.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderPaymentRepository orderPaymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentCalculationService paymentCalculationService;
    private PaymentService paymentService;
    private MockPayOSGateway payOSGateway;

    @BeforeEach
    void setUp() {
        paymentCalculationService = new PaymentCalculationService();
        payOSGateway = new MockPayOSGateway();
        paymentService = new PaymentService(
                orderPaymentRepository,
                orderRepository,
                payOSGateway,
                paymentCalculationService,
                eventPublisher
        );
    }

    @Test
    void initializeOrderPaymentStateSetsPendingBalanceFromTotalAmount() {
        Order order = Order.builder()
                .type(OrderType.NORMAL)
                .totalAmount(new BigDecimal("250.5"))
                .build();

        paymentService.initializeOrderPaymentState(order);

        assertThat(order.getPaidAmount()).isEqualByComparingTo("0.00");
        assertThat(order.getRemainingAmount()).isEqualByComparingTo("250.50");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void createPaymentCreatesMockCheckoutAndRecalculatesOrder() {
        Order order = Order.builder()
                .id(5L)
                .orderCode("SOBU-ORD-1")
                .type(OrderType.NORMAL)
                .totalAmount(new BigDecimal("1000"))
                .depositAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("1000"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            payment.setId(99L);
            return payment;
        });
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(), List.of(
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.FULL)
                        .status(PaymentStatus.PENDING)
                        .amount(new BigDecimal("1000"))
                        .build()
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment payment = paymentService.createPayment(order, PaymentType.FULL);

        assertThat(payment.getId()).isEqualTo(99L);
        assertThat(payment.getPaymentCode()).startsWith("SOBU-PAY-");
        assertThat(payment.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(payment.getProvider()).isEqualTo("PAYOS_MOCK");
        assertThat(payment.getCheckoutUrl()).contains(payment.getPaymentCode());
        assertThat(payment.getQrCode()).contains("SOBU-ORD-1");
        verify(orderRepository).save(order);
    }

    @Test
    void createPaymentRegeneratesProviderOrderCodeWhenPayOSReportsDuplicate() {
        List<Long> attemptedProviderOrderCodes = new ArrayList<>();
        int[] checkoutAttempts = {0};
        PayOSGateway duplicateThenSuccessGateway = new PayOSGateway() {
            @Override
            public String providerName() {
                return "PAYOS_TEST";
            }

            @Override
            public PayOSCheckoutSession createCheckout(Order order, OrderPayment payment) {
                attemptedProviderOrderCodes.add(payment.getProviderOrderCode());
                checkoutAttempts[0]++;
                if (checkoutAttempts[0] == 1) {
                    throw new IllegalStateException("PayOS checkout creation failed: Đơn thanh toán đã tồn tại");
                }
                return new PayOSCheckoutSession(
                        "payos-link-2",
                        "https://pay.payos.vn/web/checkout/retry",
                        "QR-RETRY",
                        LocalDateTime.now().plusMinutes(5)
                );
            }
        };
        PaymentService retryingPaymentService = new PaymentService(
                orderPaymentRepository,
                orderRepository,
                duplicateThenSuccessGateway,
                paymentCalculationService,
                eventPublisher
        );
        Order order = Order.builder()
                .id(6L)
                .orderCode("SOBU-ORD-6")
                .type(OrderType.NORMAL)
                .totalAmount(new BigDecimal("1000"))
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("1000"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        List<OrderPayment> storedPayments = new ArrayList<>();

        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.findByProviderOrderCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(6L))
                .thenAnswer(invocation -> List.copyOf(storedPayments));
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(6L);
                storedPayments.add(payment);
            }
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment payment = retryingPaymentService.createPayment(order, PaymentType.FULL, PaymentMethod.ONLINE);

        assertThat(attemptedProviderOrderCodes).hasSize(2);
        assertThat(attemptedProviderOrderCodes.get(1)).isGreaterThan(attemptedProviderOrderCodes.get(0));
        assertThat(payment.getProviderOrderCode()).isEqualTo(attemptedProviderOrderCodes.get(1));
        assertThat(payment.getProviderOrderCode()).isNotEqualTo(payment.getId());
        assertThat(payment.getCheckoutUrl()).isEqualTo("https://pay.payos.vn/web/checkout/retry");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void createPaymentPublishesSyncEventForNormalCodPaymentBeforePaid() {
        Order order = Order.builder()
                .id(19L)
                .orderCode("SOBU-ORD-19")
                .type(OrderType.NORMAL)
                .syncStatus(OrderSyncStatus.PENDING)
                .totalAmount(new BigDecimal("400"))
                .remainingAmount(new BigDecimal("400"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(191L);
            }
            return payment;
        });
        when(orderRepository.findById(19L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(19L)).thenReturn(List.of(), List.of(
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.FULL)
                        .paymentMethod(PaymentMethod.COD)
                        .status(PaymentStatus.PENDING)
                        .amount(new BigDecimal("400"))
                        .build()
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment payment = paymentService.createPayment(order, PaymentType.FULL, PaymentMethod.COD);

        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        ArgumentCaptor<OrderReadyForSyncEvent> eventCaptor = ArgumentCaptor.forClass(OrderReadyForSyncEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(19L);
        assertThat(eventCaptor.getValue().paymentCode()).isEqualTo(payment.getPaymentCode());
    }

    @Test
    void createPaymentMovesPreorderFinalCodToProcessingAndPublishesSyncEvent() {
        Order order = Order.builder()
                .id(20L)
                .orderCode("SOBU-PRE-20")
                .type(OrderType.PREORDER)
                .status(OrderStatus.READY_FOR_FINAL_PAYMENT)
                .syncStatus(OrderSyncStatus.PENDING)
                .nhanhSyncStage(com.vn.sodu.order.NhanhSyncStage.PREORDER_DEPOSIT_CREATED)
                .totalAmount(new BigDecimal("1000"))
                .depositAmount(new BigDecimal("300"))
                .paidAmount(new BigDecimal("300"))
                .remainingAmount(new BigDecimal("700"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(201L);
            }
            return payment;
        });
        when(orderRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(20L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(20L)).thenReturn(List.of(), List.of(
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.FINAL)
                        .paymentMethod(PaymentMethod.COD)
                        .status(PaymentStatus.PENDING)
                        .amount(new BigDecimal("700"))
                        .build()
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment payment = paymentService.createPayment(order, PaymentType.FINAL, PaymentMethod.COD);

        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(order.getSyncStatus()).isEqualTo(OrderSyncStatus.PENDING);
        ArgumentCaptor<OrderReadyForSyncEvent> eventCaptor = ArgumentCaptor.forClass(OrderReadyForSyncEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(20L);
    }

    @Test
    void createPaymentExpiresOldPendingFullPaymentBeforeAllowingReplacement() {
        Order order = Order.builder()
                .id(15L)
                .orderCode("SOBU-ORD-15")
                .type(OrderType.NORMAL)
                .totalAmount(new BigDecimal("1000"))
                .depositAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("1000"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment expiredPayment = OrderPayment.builder()
                .id(55L)
                .order(order)
                .paymentCode("SOBU-PAY-OLD")
                .type(PaymentType.FULL)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("1000"))
                .provider("PAYOS_MOCK")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        List<OrderPayment> storedPayments = new ArrayList<>();
        storedPayments.add(expiredPayment);

        when(orderRepository.findById(15L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(15L))
                .thenAnswer(invocation -> List.copyOf(storedPayments));
        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(156L);
                storedPayments.add(payment);
            }
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment replacement = paymentService.createPayment(order, PaymentType.FULL, PaymentMethod.ONLINE);

        assertThat(expiredPayment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(expiredPayment.getFailureReason()).isEqualTo("Payment session expired");
        assertThat(replacement.getId()).isEqualTo(156L);
        assertThat(replacement.getPaymentCode()).startsWith("SOBU-PAY-");
        assertThat(replacement.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void createPaymentRejectsExistingPendingFullPayment() {
        Order order = Order.builder()
                .id(16L)
                .orderCode("SOBU-ORD-16")
                .type(OrderType.NORMAL)
                .totalAmount(new BigDecimal("1000"))
                .depositAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("1000"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment pendingPayment = OrderPayment.builder()
                .id(56L)
                .order(order)
                .paymentCode("SOBU-PAY-PENDING")
                .type(PaymentType.FULL)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("1000"))
                .provider("PAYOS_MOCK")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(orderRepository.findById(16L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(16L)).thenReturn(List.of(pendingPayment));

        assertThrows(IllegalStateException.class,
                () -> paymentService.createPayment(order, PaymentType.FULL, PaymentMethod.ONLINE));
    }

    @Test
    void createPaymentRejectsWrongPhasePaymentForPreorder() {
        Order order = Order.builder()
                .id(7L)
                .orderCode("SOBU-PRE-1")
                .type(OrderType.PREORDER)
                .status(OrderStatus.WAITING_DEPOSIT)
                .depositAmount(new BigDecimal("200"))
                .totalAmount(new BigDecimal("1000"))
                .build();

        when(orderRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(7L)).thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> paymentService.createPayment(order, PaymentType.FINAL));
    }

    @Test
    void createPreorderFinalPaymentMovesOrderToReadyStateAndCreatesFinalCheckout() {
        Order order = Order.builder()
                .id(8L)
                .orderCode("SOBU-PRE-8")
                .type(OrderType.PREORDER)
                .status(OrderStatus.DEPOSIT_PAID)
                .totalAmount(new BigDecimal("1000"))
                .depositAmount(new BigDecimal("300"))
                .paidAmount(new BigDecimal("300"))
                .remainingAmount(new BigDecimal("700"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(orderRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(8L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(8L)).thenReturn(List.of(), List.of(
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.DEPOSIT)
                        .status(PaymentStatus.PAID)
                        .amount(new BigDecimal("300"))
                        .build(),
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.FINAL)
                        .status(PaymentStatus.PENDING)
                        .amount(new BigDecimal("700"))
                        .build()
        ));
        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            payment.setId(108L);
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment payment = paymentService.createPreorderFinalPayment(8L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_FINAL_PAYMENT);
        assertThat(payment.getType()).isEqualTo(PaymentType.FINAL);
        assertThat(payment.getAmount()).isEqualByComparingTo("700.00");
        assertThat(payment.getCheckoutUrl()).contains(payment.getPaymentCode());
    }

    @Test
    void createPaymentCreatesFinalCheckoutWhenPreorderDepositAlreadyPaid() {
        Order order = Order.builder()
                .id(18L)
                .orderCode("SOBU-PRE-18")
                .type(OrderType.PREORDER)
                .status(OrderStatus.DEPOSIT_PAID)
                .totalAmount(new BigDecimal("1200"))
                .depositAmount(new BigDecimal("300"))
                .paidAmount(new BigDecimal("300"))
                .remainingAmount(new BigDecimal("900"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(orderRepository.findByIdForUpdate(18L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(18L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(18L)).thenReturn(List.of(), List.of(
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.DEPOSIT)
                        .status(PaymentStatus.PAID)
                        .amount(new BigDecimal("300"))
                        .build(),
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.FINAL)
                        .status(PaymentStatus.PENDING)
                        .amount(new BigDecimal("900"))
                        .build()
        ));
        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            payment.setId(118L);
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment payment = paymentService.createPayment(order, PaymentType.FINAL, PaymentMethod.ONLINE);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_FINAL_PAYMENT);
        assertThat(payment.getType()).isEqualTo(PaymentType.FINAL);
        assertThat(payment.getProviderOrderCode()).isEqualTo(118L);
        assertThat(payment.getCheckoutUrl()).contains(payment.getPaymentCode());
    }

    @Test
    void createPreorderFinalPaymentFailsCheckoutAndRestoresDepositPaidStatus() {
        PayOSGateway failingGateway = new PayOSGateway() {
            @Override
            public String providerName() {
                return "PAYOS_FAIL";
            }

            @Override
            public PayOSCheckoutSession createCheckout(Order order, OrderPayment payment) {
                throw new IllegalStateException("PayOS downtime");
            }
        };
        PaymentService failingPaymentService = new PaymentService(
                orderPaymentRepository,
                orderRepository,
                failingGateway,
                paymentCalculationService,
                eventPublisher
        );

        Order order = Order.builder()
                .id(28L)
                .orderCode("SOBU-PRE-28")
                .type(OrderType.PREORDER)
                .status(OrderStatus.DEPOSIT_PAID)
                .totalAmount(new BigDecimal("1000"))
                .depositAmount(new BigDecimal("300"))
                .paidAmount(new BigDecimal("300"))
                .remainingAmount(new BigDecimal("700"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        List<OrderPayment> storedPayments = new ArrayList<>();

        when(orderRepository.findByIdForUpdate(28L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(28L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(28L))
                .thenAnswer(invocation -> List.copyOf(storedPayments));
        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(281L);
                storedPayments.add(payment);
            }
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentCheckoutCreationException ex = assertThrows(
                PaymentCheckoutCreationException.class,
                () -> failingPaymentService.createPreorderFinalPayment(28L)
        );

        assertThat(ex).hasMessageContaining("PayOS downtime");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DEPOSIT_PAID);
        assertThat(storedPayments).hasSize(1);
        assertThat(storedPayments.get(0).getType()).isEqualTo(PaymentType.FINAL);
        assertThat(storedPayments.get(0).getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(storedPayments.get(0).getFailureReason()).isEqualTo("PayOS downtime");
    }

    @Test
    void createPaymentFinalOnlineFromDepositPaidFailsCheckoutAndRestoresDepositPaidStatus() {
        PayOSGateway failingGateway = new PayOSGateway() {
            @Override
            public String providerName() {
                return "PAYOS_FAIL";
            }

            @Override
            public PayOSCheckoutSession createCheckout(Order order, OrderPayment payment) {
                throw new IllegalStateException("Network timeout");
            }
        };
        PaymentService failingPaymentService = new PaymentService(
                orderPaymentRepository,
                orderRepository,
                failingGateway,
                paymentCalculationService,
                eventPublisher
        );

        Order order = Order.builder()
                .id(29L)
                .orderCode("SOBU-PRE-29")
                .type(OrderType.PREORDER)
                .status(OrderStatus.DEPOSIT_PAID)
                .totalAmount(new BigDecimal("1200"))
                .depositAmount(new BigDecimal("300"))
                .paidAmount(new BigDecimal("300"))
                .remainingAmount(new BigDecimal("900"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        List<OrderPayment> storedPayments = new ArrayList<>();

        when(orderRepository.findByIdForUpdate(29L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(29L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(29L))
                .thenAnswer(invocation -> List.copyOf(storedPayments));
        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(291L);
                storedPayments.add(payment);
            }
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentCheckoutCreationException ex = assertThrows(
                PaymentCheckoutCreationException.class,
                () -> failingPaymentService.createPayment(order, PaymentType.FINAL, PaymentMethod.ONLINE)
        );

        assertThat(ex).hasMessageContaining("Network timeout");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DEPOSIT_PAID);
        assertThat(storedPayments).hasSize(1);
        assertThat(storedPayments.get(0).getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void createPaymentRejectsExistingPendingFinalPayment() {
        Order order = Order.builder()
                .id(30L)
                .orderCode("SOBU-PRE-30")
                .type(OrderType.PREORDER)
                .status(OrderStatus.READY_FOR_FINAL_PAYMENT)
                .depositAmount(new BigDecimal("300"))
                .totalAmount(new BigDecimal("1000"))
                .paidAmount(new BigDecimal("300"))
                .remainingAmount(new BigDecimal("700"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment pendingFinalPayment = OrderPayment.builder()
                .id(301L)
                .order(order)
                .paymentCode("SOBU-PAY-FINAL-PENDING")
                .type(PaymentType.FINAL)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("700"))
                .provider("PAYOS_MOCK")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(orderRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(30L)).thenReturn(List.of(pendingFinalPayment));

        assertThrows(IllegalStateException.class,
                () -> paymentService.createPayment(order, PaymentType.FINAL, PaymentMethod.ONLINE));
    }

    @Test
    void createPaymentAllowsRetryAfterFailedFinalPayment() {
        Order order = Order.builder()
                .id(31L)
                .orderCode("SOBU-PRE-31")
                .type(OrderType.PREORDER)
                .status(OrderStatus.DEPOSIT_PAID)
                .depositAmount(new BigDecimal("300"))
                .totalAmount(new BigDecimal("1000"))
                .paidAmount(new BigDecimal("300"))
                .remainingAmount(new BigDecimal("700"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment failedFinalPayment = OrderPayment.builder()
                .id(311L)
                .order(order)
                .paymentCode("SOBU-PAY-FINAL-FAILED")
                .type(PaymentType.FINAL)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.FAILED)
                .amount(new BigDecimal("700"))
                .provider("PAYOS_MOCK")
                .failureReason("Provider error")
                .build();

        when(orderRepository.findByIdForUpdate(31L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(31L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(31L)).thenReturn(List.of(failedFinalPayment), List.of(
                failedFinalPayment,
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.FINAL)
                        .status(PaymentStatus.PENDING)
                        .amount(new BigDecimal("700"))
                        .build()
        ));
        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(312L);
            }
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment payment = paymentService.createPayment(order, PaymentType.FINAL, PaymentMethod.ONLINE);

        assertThat(payment.getId()).isEqualTo(312L);
        assertThat(payment.getCheckoutUrl()).contains(payment.getPaymentCode());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_FINAL_PAYMENT);
    }

    @Test
    void createPaymentAllowsRetryAfterExpiredFinalPayment() {
        Order order = Order.builder()
                .id(32L)
                .orderCode("SOBU-PRE-32")
                .type(OrderType.PREORDER)
                .status(OrderStatus.DEPOSIT_PAID)
                .depositAmount(new BigDecimal("300"))
                .totalAmount(new BigDecimal("1000"))
                .paidAmount(new BigDecimal("300"))
                .remainingAmount(new BigDecimal("700"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment expiredFinalPayment = OrderPayment.builder()
                .id(321L)
                .order(order)
                .paymentCode("SOBU-PAY-FINAL-EXPIRED")
                .type(PaymentType.FINAL)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.EXPIRED)
                .amount(new BigDecimal("700"))
                .provider("PAYOS_MOCK")
                .failureReason("Payment session expired")
                .build();

        when(orderRepository.findByIdForUpdate(32L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(32L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(32L)).thenReturn(List.of(expiredFinalPayment), List.of(
                expiredFinalPayment,
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.FINAL)
                        .status(PaymentStatus.PENDING)
                        .amount(new BigDecimal("700"))
                        .build()
        ));
        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(322L);
            }
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment payment = paymentService.createPayment(order, PaymentType.FINAL, PaymentMethod.ONLINE);

        assertThat(payment.getId()).isEqualTo(322L);
        assertThat(payment.getCheckoutUrl()).contains(payment.getPaymentCode());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_FINAL_PAYMENT);
    }

    @Test
    void createPaymentLocksOrderForFinalPaymentCreation() {
        Order order = Order.builder()
                .id(33L)
                .orderCode("SOBU-PRE-33")
                .type(OrderType.PREORDER)
                .status(OrderStatus.DEPOSIT_PAID)
                .depositAmount(new BigDecimal("300"))
                .totalAmount(new BigDecimal("1000"))
                .paidAmount(new BigDecimal("300"))
                .remainingAmount(new BigDecimal("700"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(orderRepository.findByIdForUpdate(33L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(33L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(33L)).thenReturn(List.of(), List.of(
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.FINAL)
                        .status(PaymentStatus.PENDING)
                        .amount(new BigDecimal("700"))
                        .build()
        ));
        when(orderPaymentRepository.findByPaymentCode(any())).thenReturn(Optional.empty());
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> {
            OrderPayment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(331L);
            }
            return payment;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createPayment(order, PaymentType.FINAL, PaymentMethod.ONLINE);

        verify(orderRepository).findByIdForUpdate(33L);
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    void markPaymentPaidRecalculatesOrderAndPublishesSyncEventForEligibleOrder() {
        Order order = Order.builder()
                .id(5L)
                .orderCode("SOBU-ORD-1")
                .type(OrderType.NORMAL)
                .syncStatus(OrderSyncStatus.PENDING)
                .totalAmount(new BigDecimal("300"))
                .depositAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("300"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment payment = OrderPayment.builder()
                .id(91L)
                .order(order)
                .paymentCode("SOBU-PAY-91")
                .type(PaymentType.FULL)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("300"))
                .provider("PAYOS_MOCK")
                .build();

        when(orderPaymentRepository.findByPaymentCodeForUpdate("SOBU-PAY-91")).thenReturn(Optional.of(payment));
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(payment));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment updated = paymentService.markPaymentPaid("SOBU-PAY-91");

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updated.getPaidAt()).isNotNull();
        assertThat(order.getPaidAmount()).isEqualByComparingTo("300.00");
        assertThat(order.getRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        ArgumentCaptor<OrderReadyForSyncEvent> eventCaptor = ArgumentCaptor.forClass(OrderReadyForSyncEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(5L);
        assertThat(eventCaptor.getValue().paymentCode()).isEqualTo("SOBU-PAY-91");
    }

    @Test
    void markPaymentPaidMovesPreorderFromWaitingDepositToDepositPaidWithoutSyncEvent() {
        Order order = Order.builder()
                .id(6L)
                .orderCode("SOBU-PRE-6")
                .type(OrderType.PREORDER)
                .status(OrderStatus.WAITING_DEPOSIT)
                .syncStatus(OrderSyncStatus.PENDING)
                .totalAmount(new BigDecimal("1000"))
                .depositAmount(new BigDecimal("300"))
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("1000"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment payment = OrderPayment.builder()
                .id(93L)
                .order(order)
                .paymentCode("SOBU-PAY-93")
                .type(PaymentType.DEPOSIT)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("300"))
                .provider("PAYOS_MOCK")
                .build();

        when(orderPaymentRepository.findByPaymentCodeForUpdate("SOBU-PAY-93")).thenReturn(Optional.of(payment));
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(6L)).thenReturn(List.of(payment));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment updated = paymentService.markPaymentPaid("SOBU-PAY-93");

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updated.getOrder().getStatus()).isEqualTo(OrderStatus.DEPOSIT_PAID);
        assertThat(updated.getOrder().getSyncStatus()).isEqualTo(OrderSyncStatus.PENDING);
        ArgumentCaptor<OrderReadyForSyncEvent> eventCaptor = ArgumentCaptor.forClass(OrderReadyForSyncEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(6L);
        assertThat(eventCaptor.getValue().paymentCode()).isEqualTo("SOBU-PAY-93");
    }

    @Test
    void markPaymentPaidMovesReadyPreorderToProcessingAndPublishesSyncEvent() {
        Order order = Order.builder()
                .id(12L)
                .orderCode("SOBU-PRE-12")
                .type(OrderType.PREORDER)
                .status(OrderStatus.READY_FOR_FINAL_PAYMENT)
                .syncStatus(OrderSyncStatus.PENDING)
                .totalAmount(new BigDecimal("1000"))
                .depositAmount(new BigDecimal("300"))
                .paidAmount(new BigDecimal("300"))
                .remainingAmount(new BigDecimal("700"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment payment = OrderPayment.builder()
                .id(94L)
                .order(order)
                .paymentCode("SOBU-PAY-94")
                .type(PaymentType.FINAL)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("700"))
                .provider("PAYOS_MOCK")
                .build();

        when(orderPaymentRepository.findByPaymentCodeForUpdate("SOBU-PAY-94")).thenReturn(Optional.of(payment));
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(12L)).thenReturn(List.of(
                OrderPayment.builder()
                        .order(order)
                        .type(PaymentType.DEPOSIT)
                        .status(PaymentStatus.PAID)
                        .amount(new BigDecimal("300"))
                        .provider("PAYOS_MOCK")
                        .build(),
                payment
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment updated = paymentService.markPaymentPaid("SOBU-PAY-94");

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updated.getOrder().getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(updated.getOrder().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updated.getOrder().getSyncStatus()).isEqualTo(OrderSyncStatus.PENDING);
        ArgumentCaptor<OrderReadyForSyncEvent> eventCaptor = ArgumentCaptor.forClass(OrderReadyForSyncEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(12L);
        assertThat(eventCaptor.getValue().paymentCode()).isEqualTo("SOBU-PAY-94");
    }

    @Test
    void markPaymentPaidDoesNotRepublishWhenPaymentAlreadyPaid() {
        Order order = Order.builder()
                .id(5L)
                .type(OrderType.NORMAL)
                .syncStatus(OrderSyncStatus.PENDING)
                .build();
        OrderPayment payment = OrderPayment.builder()
                .id(92L)
                .order(order)
                .paymentCode("SOBU-PAY-92")
                .type(PaymentType.FULL)
                .status(PaymentStatus.PAID)
                .amount(new BigDecimal("1000"))
                .provider("PAYOS_MOCK")
                .build();

        when(orderPaymentRepository.findByPaymentCodeForUpdate("SOBU-PAY-92")).thenReturn(Optional.of(payment));

        OrderPayment updated = paymentService.markPaymentPaid("SOBU-PAY-92");

        assertThat(updated).isSameAs(payment);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void markPaymentFailedRecalculatesOrderAndAllowsRetry() {
        Order order = Order.builder()
                .id(25L)
                .orderCode("SOBU-ORD-25")
                .type(OrderType.NORMAL)
                .totalAmount(new BigDecimal("500"))
                .remainingAmount(new BigDecimal("500"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment payment = OrderPayment.builder()
                .id(251L)
                .order(order)
                .paymentCode("SOBU-PAY-251")
                .type(PaymentType.FULL)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("500"))
                .provider("PAYOS_MOCK")
                .build();

        when(orderPaymentRepository.findByPaymentCodeForUpdate("SOBU-PAY-251")).thenReturn(Optional.of(payment));
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(25L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(25L)).thenReturn(List.of(payment));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment failed = paymentService.markPaymentFailed("SOBU-PAY-251", "Payment cancelled by customer");

        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("Payment cancelled by customer");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void reconcilePendingOnlinePaymentsMarksPaymentPaidWhenProviderReportsPaid() {
        Order order = Order.builder()
                .id(41L)
                .orderCode("SOBU-ORD-41")
                .type(OrderType.NORMAL)
                .status(OrderStatus.NEW)
                .syncStatus(OrderSyncStatus.PENDING)
                .totalAmount(new BigDecimal("500"))
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("500"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment payment = OrderPayment.builder()
                .id(410L)
                .order(order)
                .paymentCode("SOBU-PAY-410")
                .providerOrderCode(410L)
                .type(PaymentType.FULL)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("500"))
                .provider("PAYOS_MOCK")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();
        payOSGateway.markPaid(410L);

        when(orderPaymentRepository.findByStatusInAndPaymentMethodOrderByCreatedAtAsc(
                any(),
                eq(PaymentMethod.ONLINE),
                any()
        )).thenReturn(List.of(payment));
        when(orderPaymentRepository.findByPaymentCodeForUpdate("SOBU-PAY-410")).thenReturn(Optional.of(payment));
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(41L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(41L)).thenReturn(List.of(payment));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reconciliationService().reconcileBatch();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaidAmount()).isEqualByComparingTo("500.00");
        assertThat(order.getRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        ArgumentCaptor<OrderReadyForSyncEvent> eventCaptor = ArgumentCaptor.forClass(OrderReadyForSyncEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(41L);
        assertThat(eventCaptor.getValue().paymentCode()).isEqualTo("SOBU-PAY-410");
    }

    @Test
    void reconcilePendingOnlinePaymentsMarksExpiredPaymentWithoutPublishingSync() {
        Order order = Order.builder()
                .id(42L)
                .orderCode("SOBU-ORD-42")
                .type(OrderType.NORMAL)
                .syncStatus(OrderSyncStatus.PENDING)
                .totalAmount(new BigDecimal("500"))
                .remainingAmount(new BigDecimal("500"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        OrderPayment payment = OrderPayment.builder()
                .id(420L)
                .order(order)
                .paymentCode("SOBU-PAY-420")
                .providerOrderCode(420L)
                .type(PaymentType.FULL)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("500"))
                .provider("PAYOS_MOCK")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        payOSGateway.markExpired(420L);

        when(orderPaymentRepository.findByStatusInAndPaymentMethodOrderByCreatedAtAsc(
                any(),
                eq(PaymentMethod.ONLINE),
                any()
        )).thenReturn(List.of(payment));
        when(orderPaymentRepository.findByPaymentCodeForUpdate("SOBU-PAY-420")).thenReturn(Optional.of(payment));
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(42L)).thenReturn(List.of(payment));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reconciliationService().reconcileBatch();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        verifyNoInteractions(eventPublisher);
    }

    private PaymentReconciliationService reconciliationService() {
        PayOSProperties properties = new PayOSProperties();
        properties.getReconciliation().setStaleAfterSeconds(0);
        return new PaymentReconciliationService(
                orderPaymentRepository,
                payOSGateway,
                properties,
                paymentService
        );
    }
}
