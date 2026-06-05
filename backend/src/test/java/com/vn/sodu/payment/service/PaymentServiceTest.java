package com.vn.sodu.payment.service;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.OrderReadyForSyncEvent;
import com.vn.sodu.order.OrderSyncStatus;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.payment.MockPayOSGateway;
import com.vn.sodu.payment.OrderPayment;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @BeforeEach
    void setUp() {
        paymentCalculationService = new PaymentCalculationService();
        paymentService = new PaymentService(
                orderPaymentRepository,
                orderRepository,
                new MockPayOSGateway(),
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
    void createPaymentRejectsWrongPhasePaymentForPreorder() {
        Order order = Order.builder()
                .id(7L)
                .orderCode("SOBU-PRE-1")
                .type(OrderType.PREORDER)
                .status(OrderStatus.WAITING_DEPOSIT)
                .depositAmount(new BigDecimal("200"))
                .totalAmount(new BigDecimal("1000"))
                .build();

        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
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

        when(orderRepository.findById(8L)).thenReturn(Optional.of(order), Optional.of(order));
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

        when(orderPaymentRepository.findByPaymentCode("SOBU-PAY-91")).thenReturn(Optional.of(payment));
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(payment));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment updated = paymentService.markPaymentPaid("SOBU-PAY-91");

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updated.getPaidAt()).isNotNull();
        assertThat(order.getPaidAmount()).isEqualByComparingTo("300.00");
        assertThat(order.getRemainingAmount()).isEqualByComparingTo("0.00");

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

        when(orderPaymentRepository.findByPaymentCode("SOBU-PAY-93")).thenReturn(Optional.of(payment));
        when(orderPaymentRepository.save(any(OrderPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
        when(orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(6L)).thenReturn(List.of(payment));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPayment updated = paymentService.markPaymentPaid("SOBU-PAY-93");

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updated.getOrder().getStatus()).isEqualTo(OrderStatus.DEPOSIT_PAID);
        verifyNoInteractions(eventPublisher);
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

        when(orderPaymentRepository.findByPaymentCode("SOBU-PAY-94")).thenReturn(Optional.of(payment));
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

        when(orderPaymentRepository.findByPaymentCode("SOBU-PAY-92")).thenReturn(Optional.of(payment));

        OrderPayment updated = paymentService.markPaymentPaid("SOBU-PAY-92");

        assertThat(updated).isSameAs(payment);
        verifyNoInteractions(eventPublisher);
    }
}
