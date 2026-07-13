package com.vn.sodu.payment.service;

import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PayOSGateway;
import com.vn.sodu.payment.PayOSPaymentStatusSnapshot;
import com.vn.sodu.payment.PayOSProperties;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.dto.PaymentReconciliationResult;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceTest {

    @Mock
    private OrderPaymentRepository orderPaymentRepository;

    @Mock
    private PayOSGateway payOSGateway;

    @Mock
    private PaymentService paymentService;

    private PaymentReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        PayOSProperties properties = new PayOSProperties();
        properties.getReconciliation().setStaleAfterSeconds(0);
        properties.getReconciliation().setBatchSize(20);
        reconciliationService = new PaymentReconciliationService(
                orderPaymentRepository,
                payOSGateway,
                properties,
                paymentService
        );
    }

    @Test
    void reconcilesPaidAndExpiredThroughPaymentServiceProxy() {
        OrderPayment paid = payment("SOBU-PAY-PAID", 101L);
        OrderPayment expired = payment("SOBU-PAY-EXPIRED", 102L);
        mockCandidates(List.of(paid, expired));
        when(payOSGateway.getPaymentStatus(101L)).thenReturn(snapshot(101L, PaymentStatus.PAID, "PAID"));
        when(payOSGateway.getPaymentStatus(102L)).thenReturn(snapshot(102L, PaymentStatus.EXPIRED, "EXPIRED"));

        PaymentReconciliationResult result = reconciliationService.reconcileBatch();

        assertThat(result).isEqualTo(new PaymentReconciliationResult(2, 0, 1, 1, 0, 0));
        verify(paymentService).markPaymentPaid("SOBU-PAY-PAID");
        verify(paymentService).markPaymentExpired("SOBU-PAY-EXPIRED", "Payment session expired");
    }

    @Test
    void recordsProviderErrorsAndContinuesWithFollowingPayments() {
        OrderPayment failing = payment("SOBU-PAY-FAIL", 201L);
        OrderPayment paid = payment("SOBU-PAY-NEXT", 202L);
        mockCandidates(List.of(failing, paid));
        doThrow(new IllegalStateException("provider unavailable")).when(payOSGateway).getPaymentStatus(201L);
        when(payOSGateway.getPaymentStatus(202L)).thenReturn(snapshot(202L, PaymentStatus.PAID, "PAID"));

        PaymentReconciliationResult result = reconciliationService.reconcileBatch();

        assertThat(result).isEqualTo(new PaymentReconciliationResult(2, 0, 1, 0, 0, 1));
        verify(paymentService).markPaymentPaid("SOBU-PAY-NEXT");
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void countsMissingProviderStatusWithoutMutatingUnexpiredPayment() {
        OrderPayment pending = payment("SOBU-PAY-PENDING", 301L);
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        mockCandidates(List.of(pending));
        when(payOSGateway.getPaymentStatus(301L)).thenReturn(null);

        PaymentReconciliationResult result = reconciliationService.reconcileBatch();

        assertThat(result).isEqualTo(new PaymentReconciliationResult(1, 0, 0, 0, 1, 0));
        verifyNoMoreInteractions(paymentService);
    }

    private void mockCandidates(List<OrderPayment> payments) {
        when(orderPaymentRepository.findByStatusInAndPaymentMethodOrderByCreatedAtAsc(
                eq(java.util.EnumSet.of(PaymentStatus.PENDING, PaymentStatus.FAILED)),
                eq(PaymentMethod.ONLINE),
                any(Pageable.class)
        )).thenReturn(payments);
    }

    private OrderPayment payment(String paymentCode, long providerOrderCode) {
        return OrderPayment.builder()
                .paymentCode(paymentCode)
                .providerOrderCode(providerOrderCode)
                .type(PaymentType.FULL)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("15000"))
                .provider("PAYOS")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    private PayOSPaymentStatusSnapshot snapshot(long providerOrderCode, PaymentStatus status, String rawStatus) {
        return new PayOSPaymentStatusSnapshot(providerOrderCode, status, "reference-" + providerOrderCode, null, rawStatus);
    }
}
