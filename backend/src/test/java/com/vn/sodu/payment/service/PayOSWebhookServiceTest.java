package com.vn.sodu.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.PaymentWebhookEvent;
import com.vn.sodu.payment.PaymentWebhookStatus;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import com.vn.sodu.payment.repo.PaymentWebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayOSWebhookServiceTest {

    @Mock
    private PaymentWebhookEventRepository paymentWebhookEventRepository;

    @Mock
    private OrderPaymentRepository orderPaymentRepository;

    @Mock
    private PaymentService paymentService;

    private PayOSWebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new PayOSWebhookService(
                new ObjectMapper(),
                paymentWebhookEventRepository,
                orderPaymentRepository,
                paymentService
        );
        when(paymentWebhookEventRepository.save(any(PaymentWebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void receiveMarksPendingPaymentPaidFromPaymentCode() {
        OrderPayment payment = OrderPayment.builder()
                .paymentCode("SOBU-PAY-501")
                .type(PaymentType.FULL)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("500.00"))
                .providerReference("mock-payos-sobu-pay-501")
                .build();
        when(orderPaymentRepository.findByPaymentCode("SOBU-PAY-501")).thenReturn(Optional.of(payment));

        HttpHeaders headers = new HttpHeaders();
        headers.add("x-payos-signature", "sig-501");

        webhookService.receive("""
                {"event":"payment.paid","success":true,"paymentCode":"SOBU-PAY-501"}
                """, headers);

        verify(paymentService).markPaymentPaid("SOBU-PAY-501");

        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(paymentWebhookEventRepository, atLeastOnce()).save(captor.capture());
        PaymentWebhookEvent finalSave = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalSave.getStatus()).isEqualTo(PaymentWebhookStatus.PROCESSED);
        assertThat(finalSave.getPaymentCode()).isEqualTo("SOBU-PAY-501");
        assertThat(finalSave.getSignature()).isEqualTo("sig-501");
        assertThat(finalSave.getRawPayload()).contains("payment.paid");
    }

    @Test
    void receiveSkipsDuplicatePaidPayment() {
        OrderPayment payment = OrderPayment.builder()
                .paymentCode("SOBU-PAY-502")
                .type(PaymentType.FULL)
                .status(PaymentStatus.PAID)
                .amount(new BigDecimal("250.00"))
                .providerReference("mock-payos-sobu-pay-502")
                .build();
        when(orderPaymentRepository.findByPaymentCode("SOBU-PAY-502")).thenReturn(Optional.of(payment));

        webhookService.receive("""
                {"event":"payment.paid","success":true,"paymentCode":"SOBU-PAY-502"}
                """, new HttpHeaders());

        verifyNoInteractions(paymentService);

        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(paymentWebhookEventRepository, atLeastOnce()).save(captor.capture());
        PaymentWebhookEvent finalSave = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalSave.getStatus()).isEqualTo(PaymentWebhookStatus.DUPLICATE);
        assertThat(finalSave.getProcessingNote()).contains("already marked as paid");
    }

    @Test
    void receiveFallsBackToProviderReferenceLookup() {
        OrderPayment payment = OrderPayment.builder()
                .paymentCode("SOBU-PAY-503")
                .type(PaymentType.DEPOSIT)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("150.00"))
                .providerReference("mock-payos-sobu-pay-503")
                .build();
        when(orderPaymentRepository.findByProviderReference("mock-payos-sobu-pay-503")).thenReturn(Optional.of(payment));

        webhookService.receive("""
                {"type":"payment.paid","status":"PAID","data":{"providerReference":"mock-payos-sobu-pay-503"}}
                """, new HttpHeaders());

        verify(paymentService).markPaymentPaid("SOBU-PAY-503");

        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(paymentWebhookEventRepository, atLeastOnce()).save(captor.capture());
        PaymentWebhookEvent finalSave = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalSave.getStatus()).isEqualTo(PaymentWebhookStatus.PROCESSED);
        assertThat(finalSave.getProviderReference()).isEqualTo("mock-payos-sobu-pay-503");
    }
}
