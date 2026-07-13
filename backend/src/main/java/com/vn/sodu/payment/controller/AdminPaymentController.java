package com.vn.sodu.payment.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.dto.OrderPaymentResponseDto;
import com.vn.sodu.payment.dto.PaymentReconciliationResult;
import com.vn.sodu.payment.service.PaymentReconciliationService;
import com.vn.sodu.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("v1/api/admin/payments")
@Tag(name = "Admin Payments", description = "Admin endpoints for mock payment confirmation")
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final PaymentReconciliationService paymentReconciliationService;

    @PostMapping("/reconcile")
    @Operation(
            summary = "Reconcile pending PayOS payments",
            description = "Checks one configured batch of pending or failed online payments against PayOS and applies terminal provider statuses."
    )
    public ResponseEntity<ApiResponseDTO<PaymentReconciliationResult>> reconcilePayments(Authentication authentication) {
        requireStaff(authentication);
        PaymentReconciliationResult result = paymentReconciliationService.reconcileBatch();
        return ResponseEntity.ok(ApiResponseDTO.success(
                result,
                "Payment reconciliation completed",
                HttpStatus.OK.value()
        ));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(
            summary = "List payments for an order",
            description = "Returns the persisted payment history for an order to staff and admin users."
    )
    public ResponseEntity<ApiResponseDTO<List<OrderPaymentResponseDto>>> listOrderPayments(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        requireStaff(authentication);
        List<OrderPaymentResponseDto> payments = paymentService.refreshPaymentStatuses(orderId).stream()
                .map(OrderPaymentResponseDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponseDTO.success(
                payments,
                "Payments retrieved",
                HttpStatus.OK.value()
        ));
    }

    @PostMapping("/{paymentCode}/mock/confirm")
    @Operation(
            summary = "Confirm a mock payment",
            description = "Marks a mock payment as paid and triggers post-payment order sync when eligible."
    )
    public ResponseEntity<ApiResponseDTO<OrderPaymentResponseDto>> confirmMockPayment(
            @PathVariable String paymentCode,
            Authentication authentication
    ) {
        requireStaff(authentication);
        OrderPayment payment = paymentService.markPaymentPaid(paymentCode);
        return ResponseEntity.ok(ApiResponseDTO.success(
                OrderPaymentResponseDto.from(payment),
                "Payment confirmed",
                HttpStatus.OK.value()
        ));
    }

    @PostMapping("/orders/{orderId}/final")
    @Operation(
            summary = "Create final payment for a preorder",
            description = "Marks an eligible preorder as ready for final payment and creates the mock FINAL checkout session."
    )
    public ResponseEntity<ApiResponseDTO<OrderPaymentResponseDto>> createPreorderFinalPayment(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        requireStaff(authentication);
        OrderPayment payment = paymentService.createPreorderFinalPayment(orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.success(
                OrderPaymentResponseDto.from(payment),
                "Final payment created",
                HttpStatus.CREATED.value()
        ));
    }

    private void requireStaff(Authentication authentication) {
        if (!isStaff(authentication)) {
            throw new AccessDeniedException("Staff access is required");
        }
    }

    private boolean isStaff(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }
            String name = authority.getAuthority();
            if (name.equals("ROLE_ADMIN") || name.equals("ROLE_STAFF")) {
                return true;
            }
        }
        return false;
    }
}
