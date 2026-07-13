package com.vn.sodu.order.dtos;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.request.OrderType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class CustomerOrderListItemDto {
    Long id;
    String orderCode;
    OrderType type;
    OrderStatus status;
    BigDecimal totalAmount;
    BigDecimal paidAmount;
    BigDecimal remainingAmount;
    PaymentStatus paymentStatus;
    LocalDateTime createdAt;

    public static CustomerOrderListItemDto from(Order order) {
        return CustomerOrderListItemDto.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .type(order.getType())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .paidAmount(order.getPaidAmount())
                .remainingAmount(order.getRemainingAmount())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
