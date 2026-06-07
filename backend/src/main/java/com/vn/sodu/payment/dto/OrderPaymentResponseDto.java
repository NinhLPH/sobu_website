package com.vn.sodu.payment.dto;

import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OrderPaymentResponseDto {
    private Long id;
    private Long orderId;
    private String paymentCode;
    private PaymentType type;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String provider;
    private String providerReference;
    private String checkoutUrl;
    private String qrCode;
    private String failureReason;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderPaymentResponseDto from(OrderPayment payment) {
        if (payment == null) {
            return null;
        }
        return OrderPaymentResponseDto.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() == null ? null : payment.getOrder().getId())
                .paymentCode(payment.getPaymentCode())
                .type(payment.getType())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .provider(payment.getProvider())
                .providerReference(payment.getProviderReference())
                .checkoutUrl(payment.getCheckoutUrl())
                .qrCode(payment.getQrCode())
                .failureReason(payment.getFailureReason())
                .expiresAt(payment.getExpiresAt())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
