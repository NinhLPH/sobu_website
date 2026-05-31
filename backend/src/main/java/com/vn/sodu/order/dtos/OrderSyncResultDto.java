package com.vn.sodu.order.dtos;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderSyncStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderSyncResultDto {
    private Long orderId;
    private String orderCode;
    private OrderSyncStatus syncStatus;
    private String nhanhOrderId;
    private String nhanhOrderCode;
    private String syncError;

    public static OrderSyncResultDto from(Order order) {
        return OrderSyncResultDto.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .syncStatus(order.getSyncStatus())
                .nhanhOrderId(order.getNhanhOrderId())
                .nhanhOrderCode(order.getNhanhOrderCode())
                .syncError(order.getSyncError())
                .build();
    }
}
