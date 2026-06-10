package com.vn.sodu.payment.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.idempotency.IdempotencyScope;
import com.vn.sodu.global.idempotency.IdempotencyService;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import com.vn.sodu.payment.dto.CreateOrderPaymentDto;
import com.vn.sodu.payment.dto.OrderPaymentResponseDto;
import com.vn.sodu.payment.service.PaymentService;
import com.vn.sodu.request.OrderType;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentControllerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AccountRepo accountRepo;

    @Mock
    private OrderPaymentRepository orderPaymentRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private IdempotencyService idempotencyService;

    @Test
    void createPaymentReturnsCheckoutSessionForAuthenticatedCustomerOrder() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("customer@example.com", "n/a");
        Account account = Account.builder()
                .email("customer@example.com")
                .phone(null)
                .passwordHash("hashed")
                .fullName("Customer")
                .build();
        Order order = Order.builder()
                .id(15L)
                .customerMobile("0900000001")
                .customerEmail("customer@example.com")
                .type(OrderType.NORMAL)
                .build();
        OrderPayment payment = OrderPayment.builder()
                .id(25L)
                .order(order)
                .paymentCode("SOBU-PAY-1")
                .type(PaymentType.FULL)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("1000.00"))
                .provider("PAYOS_MOCK")
                .checkoutUrl("https://mock-payos.local/checkout/SOBU-PAY-1")
                .qrCode("MOCKQR:SOBU-ORD-1:SOBU-PAY-1")
                .build();
        CreateOrderPaymentDto request = new CreateOrderPaymentDto();
        request.setType(PaymentType.FULL);
        request.setPaymentMethod(PaymentMethod.ONLINE);

        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(orderRepository.findWithItemsAndRequestById(15L)).thenReturn(Optional.of(order));
        when(paymentService.createPayment(order, PaymentType.FULL, PaymentMethod.ONLINE)).thenReturn(payment);

        OrderPaymentController controller = new OrderPaymentController(orderRepository, orderPaymentRepository, accountRepo, paymentService, idempotencyService);

        ResponseEntity<ApiResponseDTO<OrderPaymentResponseDto>> response =
                controller.createPayment(15L, request, authentication, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getPaymentCode()).isEqualTo("SOBU-PAY-1");
        verify(paymentService).createPayment(order, PaymentType.FULL, PaymentMethod.ONLINE);
    }

    @Test
    void createPaymentUsesIdempotencyKeyWhenProvided() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("customer@example.com", "n/a");
        Account account = Account.builder()
                .email("customer@example.com")
                .phone(null)
                .passwordHash("hashed")
                .fullName("Customer")
                .build();
        Order order = Order.builder()
                .id(15L)
                .customerMobile("0900000001")
                .customerEmail("customer@example.com")
                .type(OrderType.NORMAL)
                .build();
        CreateOrderPaymentDto request = new CreateOrderPaymentDto();
        request.setType(PaymentType.FULL);
        request.setPaymentMethod(PaymentMethod.ONLINE);
        OrderPaymentResponseDto replayed = OrderPaymentResponseDto.builder()
                .id(25L)
                .orderId(15L)
                .paymentCode("SOBU-PAY-1")
                .type(PaymentType.FULL)
                .status(PaymentStatus.PENDING)
                .build();
        ResponseEntity<ApiResponseDTO<OrderPaymentResponseDto>> idempotentResponse = ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(replayed, "Payment created", HttpStatus.CREATED.value()));

        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(orderRepository.findWithItemsAndRequestById(15L)).thenReturn(Optional.of(order));
        when(idempotencyService.execute(
                eq(IdempotencyScope.CREATE_PAYMENT),
                eq("payment-key-1"),
                any(),
                eq(OrderPaymentResponseDto.class),
                eq("PAYMENT"),
                any(),
                any()
        )).thenReturn(idempotentResponse);

        OrderPaymentController controller = new OrderPaymentController(orderRepository, orderPaymentRepository, accountRepo, paymentService, idempotencyService);

        ResponseEntity<ApiResponseDTO<OrderPaymentResponseDto>> response =
                controller.createPayment(15L, request, authentication, "payment-key-1");

        assertThat(response).isSameAs(idempotentResponse);
        verify(idempotencyService).execute(
                eq(IdempotencyScope.CREATE_PAYMENT),
                eq("payment-key-1"),
                any(),
                eq(OrderPaymentResponseDto.class),
                eq("PAYMENT"),
                any(),
                any(Supplier.class)
        );
    }

    @Test
    void listPaymentsReturnsExistingPaymentsForAuthenticatedCustomerOrder() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("customer@example.com", "n/a");
        Account account = Account.builder()
                .email("customer@example.com")
                .phone(null)
                .passwordHash("hashed")
                .fullName("Customer")
                .build();
        Order order = Order.builder()
                .id(15L)
                .customerMobile("0900000001")
                .customerEmail("customer@example.com")
                .type(OrderType.PREORDER)
                .build();
        OrderPayment payment = OrderPayment.builder()
                .id(30L)
                .order(order)
                .paymentCode("SOBU-PAY-30")
                .type(PaymentType.DEPOSIT)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("300.00"))
                .provider("PAYOS_MOCK")
                .checkoutUrl("https://mock-payos.local/checkout/SOBU-PAY-30")
                .build();

        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(orderRepository.findWithItemsAndRequestById(15L)).thenReturn(Optional.of(order));
        when(paymentService.refreshPaymentStatuses(15L)).thenReturn(List.of(payment));

        OrderPaymentController controller = new OrderPaymentController(orderRepository, orderPaymentRepository, accountRepo, paymentService, idempotencyService);

        ResponseEntity<ApiResponseDTO<List<OrderPaymentResponseDto>>> response =
                controller.listPayments(15L, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().get(0).getPaymentCode()).isEqualTo("SOBU-PAY-30");
    }

    @Test
    void createPaymentAcceptsEquivalentEmailCasing() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("customer@example.com", "n/a");
        Account account = Account.builder()
                .email(" Customer@Example.com ")
                .phone(null)
                .passwordHash("hashed")
                .fullName("Customer")
                .build();
        Order order = Order.builder()
                .id(15L)
                .customerMobile("0900000001")
                .customerEmail("customer@example.com")
                .type(OrderType.NORMAL)
                .build();
        OrderPayment payment = OrderPayment.builder()
                .id(25L)
                .order(order)
                .paymentCode("SOBU-PAY-1")
                .type(PaymentType.FULL)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("1000.00"))
                .provider("PAYOS_MOCK")
                .checkoutUrl("https://mock-payos.local/checkout/SOBU-PAY-1")
                .build();
        CreateOrderPaymentDto request = new CreateOrderPaymentDto();
        request.setType(PaymentType.FULL);
        request.setPaymentMethod(PaymentMethod.ONLINE);

        when(accountRepo.findByEmail("customer@example.com")).thenReturn(Optional.of(account));
        when(orderRepository.findWithItemsAndRequestById(15L)).thenReturn(Optional.of(order));
        when(paymentService.createPayment(order, PaymentType.FULL, PaymentMethod.ONLINE)).thenReturn(payment);

        OrderPaymentController controller = new OrderPaymentController(orderRepository, orderPaymentRepository, accountRepo, paymentService, idempotencyService);

        ResponseEntity<ApiResponseDTO<OrderPaymentResponseDto>> response =
                controller.createPayment(15L, request, authentication, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getPaymentCode()).isEqualTo("SOBU-PAY-1");
    }
}
