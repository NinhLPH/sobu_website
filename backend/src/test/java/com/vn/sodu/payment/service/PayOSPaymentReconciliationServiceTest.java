package com.vn.sodu.payment.service;

import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PayOSGateway;
import com.vn.sodu.payment.PayOSPaymentStatusSnapshot;
import com.vn.sodu.payment.PayOSProperties;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayOSPaymentReconciliationServiceTest {

    @Mock
    private OrderPaymentRepository orderPaymentRepository;

    @Mock
    private PayOSGateway payOSGateway;

    @Mock
    private PaymentService paymentService;

    @Test
    void continuesWithLaterPaymentsWhenOneProviderLookupFails() {
        OrderPayment failedLookup = payment("SOBU-PAY-1", 1L);
        OrderPayment paidPayment = payment("SOBU-PAY-2", 2L);
        PayOSProperties properties = new PayOSProperties();
        PayOSPaymentReconciliationService service = new PayOSPaymentReconciliationService(
                orderPaymentRepository,
                payOSGateway,
                properties,
                paymentService
        );
        when(orderPaymentRepository.findByStatusInAndPaymentMethodOrderByCreatedAtAsc(
                any(), eq(PaymentMethod.ONLINE), any()
        )).thenReturn(List.of(failedLookup, paidPayment));
        doThrow(new IllegalStateException("PayOS temporarily unavailable"))
                .when(payOSGateway).getPaymentStatus(1L);
        when(payOSGateway.getPaymentStatus(2L)).thenReturn(new PayOSPaymentStatusSnapshot(
                2L,
                PaymentStatus.PAID,
                "payos-link-2",
                null,
                "PAID"
        ));

        PaymentReconciliationResult result = service.reconcilePendingOnlinePayments();

        assertThat(result).isEqualTo(new PaymentReconciliationResult(2, 0, 1, 0, 0, 1));
        verify(paymentService).markPaymentPaid("SOBU-PAY-2");
    }

    private OrderPayment payment(String paymentCode, Long providerOrderCode) {
        return OrderPayment.builder()
                .paymentCode(paymentCode)
                .providerOrderCode(providerOrderCode)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();
    }
}
