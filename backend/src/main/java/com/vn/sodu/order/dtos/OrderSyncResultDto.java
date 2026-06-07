package com.vn.sodu.order.dtos;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.NhanhSyncStage;
import com.vn.sodu.order.OrderSyncStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderSyncResultDto {
    private Long orderId;
    private String orderCode;
    private OrderSyncStatus syncStatus;
    private NhanhSyncStage nhanhSyncStage;
    private String nhanhOrderId;
    private String nhanhOrderCode;
    private String syncError;
    private String lastSyncMessage;
    private LocalDateTime lastSyncAt;

    public static OrderSyncResultDto from(Order order) {
        return OrderSyncResultDto.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .syncStatus(order.getSyncStatus())
                .nhanhSyncStage(order.getNhanhSyncStage())
                .nhanhOrderId(order.getNhanhOrderId())
                .nhanhOrderCode(order.getNhanhOrderCode())
                .syncError(order.getSyncError())
                .lastSyncMessage(order.getLastSyncMessage())
                .lastSyncAt(order.getLastSyncAt())
                .build();
    }
}
