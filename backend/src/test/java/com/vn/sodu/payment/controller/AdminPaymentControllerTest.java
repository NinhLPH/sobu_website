package com.vn.sodu.payment.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.order.Order;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.dto.OrderPaymentResponseDto;
import com.vn.sodu.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Test
    void confirmMockPaymentReturnsUpdatedPayment() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "staff@example.com",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );
        Order order = Order.builder().id(11L).build();
        OrderPayment payment = OrderPayment.builder()
                .id(31L)
                .order(order)
                .paymentCode("SOBU-PAY-31")
                .type(PaymentType.FULL)
                .status(PaymentStatus.PAID)
                .amount(new BigDecimal("500.00"))
                .provider("PAYOS_MOCK")
                .build();

        when(paymentService.markPaymentPaid("SOBU-PAY-31")).thenReturn(payment);

        AdminPaymentController controller = new AdminPaymentController(paymentService);

        ResponseEntity<ApiResponseDTO<OrderPaymentResponseDto>> response =
                controller.confirmMockPayment("SOBU-PAY-31", authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentService).markPaymentPaid("SOBU-PAY-31");
    }

    @Test
    void createPreorderFinalPaymentReturnsCreatedCheckout() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "staff@example.com",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );
        Order order = Order.builder().id(12L).build();
        OrderPayment payment = OrderPayment.builder()
                .id(32L)
                .order(order)
                .paymentCode("SOBU-PAY-32")
                .type(PaymentType.FINAL)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("700.00"))
                .provider("PAYOS_MOCK")
                .checkoutUrl("https://mock-payos.local/checkout/SOBU-PAY-32")
                .build();

        when(paymentService.createPreorderFinalPayment(12L)).thenReturn(payment);

        AdminPaymentController controller = new AdminPaymentController(paymentService);

        ResponseEntity<ApiResponseDTO<OrderPaymentResponseDto>> response =
                controller.createPreorderFinalPayment(12L, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getType()).isEqualTo(PaymentType.FINAL);
        verify(paymentService).createPreorderFinalPayment(12L);
    }
}
