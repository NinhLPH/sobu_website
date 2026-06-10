package com.vn.sodu.payment.controller;

import com.vn.sodu.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PayOSPaymentRedirectControllerTest {

    @Mock
    private PaymentService paymentService;

    @Test
    void cancelMarksPaymentFailedAndRedirects() {
        PayOSPaymentRedirectController controller = new PayOSPaymentRedirectController(paymentService);

        ResponseEntity<Void> response = controller.cancel(
                "SOBU-PAY-123",
                "http://localhost:5173/payment/cancel"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:5173/payment/cancel?paymentCode=SOBU-PAY-123&status=FAILED");
        verify(paymentService).markPaymentFailed("SOBU-PAY-123", "Payment cancelled by customer");
    }
}
