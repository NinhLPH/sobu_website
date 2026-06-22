package com.vn.sodu.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PayOSProperties;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
    private PayOSProperties payOSProperties;

    @BeforeEach
    void setUp() {
        payOSProperties = new PayOSProperties();
        payOSProperties.setChecksumKey("test-checksum-key");
        payOSProperties.setGatewayMode("mock");
        webhookService = new PayOSWebhookService(
                new ObjectMapper(),
                paymentWebhookEventRepository,
                orderPaymentRepository,
                paymentService,
                payOSProperties
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

    @Test
    void receiveRealPayOSWebhookVerifiesSignatureAndResolvesProviderOrderCode() {
        payOSProperties.setGatewayMode("real");
        OrderPayment payment = OrderPayment.builder()
                .paymentCode("SOBU-PAY-504")
                .providerOrderCode(504L)
                .type(PaymentType.FINAL)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("700.00"))
                .providerReference("link-504")
                .build();
        when(orderPaymentRepository.findByProviderOrderCode(504L)).thenReturn(Optional.of(payment));

        String data = """
                {"orderCode":504,"amount":700,"currency":"VND","paymentLinkId":"link-504","reference":"TF504","code":"00"}
                """;
        String signature = sign("amount=700&code=00&currency=VND&orderCode=504&paymentLinkId=link-504&reference=TF504");

        webhookService.receive("""
                {"code":"00","desc":"success","success":true,"data":%s,"signature":"%s"}
                """.formatted(data.trim(), signature), new HttpHeaders());

        verify(paymentService).markPaymentPaid("SOBU-PAY-504");

        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(paymentWebhookEventRepository, atLeastOnce()).save(captor.capture());
        PaymentWebhookEvent finalSave = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalSave.getStatus()).isEqualTo(PaymentWebhookStatus.PROCESSED);
        assertThat(finalSave.getPaymentCode()).isEqualTo("SOBU-PAY-504");
        assertThat(finalSave.getProviderReference()).isEqualTo("link-504");
    }

    @Test
    void receiveRealPayOSWebhookVerifiesOfficialSampleSignature() {
        payOSProperties.setGatewayMode("real");
        payOSProperties.setChecksumKey("1a54716c8f0efb2744fb28b6e38b25da7f67a925d98bc1c18bd8faaecadd7675");
        OrderPayment payment = OrderPayment.builder()
                .paymentCode("SOBU-PAY-123")
                .providerOrderCode(123L)
                .type(PaymentType.FULL)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("3000.00"))
                .providerReference("124c33293c43417ab7879e14c8d9eb18")
                .build();
        when(orderPaymentRepository.findByProviderOrderCode(123L)).thenReturn(Optional.of(payment));

        webhookService.receive("""
                {
                  "code":"00",
                  "desc":"success",
                  "success":true,
                  "data":{
                    "orderCode":123,
                    "amount":3000,
                    "description":"VQRIO123",
                    "accountNumber":"12345678",
                    "reference":"TF230204212323",
                    "transactionDateTime":"2023-02-04 18:25:00",
                    "currency":"VND",
                    "paymentLinkId":"124c33293c43417ab7879e14c8d9eb18",
                    "code":"00",
                    "desc":"Thành công",
                    "counterAccountBankId":"",
                    "counterAccountBankName":"",
                    "counterAccountName":"",
                    "counterAccountNumber":"",
                    "virtualAccountName":"",
                    "virtualAccountNumber":""
                  },
                  "signature":"412e915d2871504ed31be63c8f62a149a4410d34c4c42affc9006ef9917eaa03"
                }
                """, new HttpHeaders());

        verify(paymentService).markPaymentPaid("SOBU-PAY-123");
    }

    @Test
    void receiveRealPayOSWebhookSortsNestedArrayObjectKeysBeforeVerifyingSignature() {
        payOSProperties.setGatewayMode("real");
        OrderPayment payment = OrderPayment.builder()
                .paymentCode("SOBU-PAY-506")
                .providerOrderCode(506L)
                .type(PaymentType.FULL)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("100.00"))
                .providerReference("link-506")
                .build();
        when(orderPaymentRepository.findByProviderOrderCode(506L)).thenReturn(Optional.of(payment));
        String signature = sign("items=[{\"qty\":1,\"sku\":\"A\"}]&orderCode=506");

        webhookService.receive("""
                {
                  "success":true,
                  "data":{
                    "orderCode":506,
                    "items":[{"sku":"A","qty":1}]
                  },
                  "signature":"%s"
                }
                """.formatted(signature), new HttpHeaders());

        verify(paymentService).markPaymentPaid("SOBU-PAY-506");
    }

    @Test
    void receiveRealPayOSWebhookRejectsInvalidSignature() {
        payOSProperties.setGatewayMode("real");

        webhookService.receive("""
                {"code":"00","desc":"success","success":true,"data":{"orderCode":505,"amount":700,"currency":"VND","paymentLinkId":"link-505","reference":"TF505","code":"00"},"signature":"bad"}
                """, new HttpHeaders());

        verifyNoInteractions(paymentService);

        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(paymentWebhookEventRepository, atLeastOnce()).save(captor.capture());
        PaymentWebhookEvent finalSave = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(finalSave.getStatus()).isEqualTo(PaymentWebhookStatus.INVALID);
        assertThat(finalSave.getProcessingNote()).contains("Invalid PayOS webhook signature");
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(payOSProperties.getChecksumKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
