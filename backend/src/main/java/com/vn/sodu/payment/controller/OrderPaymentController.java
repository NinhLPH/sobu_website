package com.vn.sodu.payment.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import com.vn.sodu.payment.dto.CreateOrderPaymentDto;
import com.vn.sodu.payment.dto.OrderPaymentResponseDto;
import com.vn.sodu.payment.service.PaymentService;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/orders", "/api/orders"})
@Tag(name = "Order Payments", description = "Authenticated customer payment endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderPaymentController {

    private final OrderRepository orderRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final AccountRepo accountRepo;
    private final PaymentService paymentService;

    @GetMapping("/{orderId}/payments")
    @Operation(
            summary = "List payments for my order",
            description = "Returns the authenticated customer's payment records, including any auto-created preorder deposit checkout session."
    )
    public ResponseEntity<ApiResponseDTO<List<OrderPaymentResponseDto>>> listPayments(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String customerPhone = resolveCustomerPhone(authentication);
        Order order = orderRepository.findCustomerOrderById(orderId, customerPhone)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        List<OrderPaymentResponseDto> payments = orderPaymentRepository.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(OrderPaymentResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponseDTO.success(
                payments,
                "Payments retrieved",
                HttpStatus.OK.value()
        ));
    }

    @PostMapping("/{orderId}/payments")
    @Operation(
            summary = "Create a payment checkout session",
            description = "Creates a payment record for the authenticated customer's order when the current order phase allows it."
    )
    public ResponseEntity<ApiResponseDTO<OrderPaymentResponseDto>> createPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody CreateOrderPaymentDto dto,
            Authentication authentication
    ) {
        String customerPhone = resolveCustomerPhone(authentication);
        Order order = orderRepository.findCustomerOrderById(orderId, customerPhone)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        OrderPayment payment = paymentService.createPayment(order, dto.getType(), dto.getPaymentMethod());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(
                        OrderPaymentResponseDto.from(payment),
                        "Payment created",
                        HttpStatus.CREATED.value()
                ));
    }

    private String resolveCustomerPhone(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Account account = accountRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated account not found"));

        if (account.getPhone() == null || account.getPhone().isBlank()) {
            throw new AccessDeniedException("Authenticated account does not have a phone number");
        }
        return account.getPhone().trim();
    }
}
