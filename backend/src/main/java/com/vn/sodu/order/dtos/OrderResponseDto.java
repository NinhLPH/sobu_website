package com.vn.sodu.order.dtos;

import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.OrderSyncStatus;
import com.vn.sodu.request.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponseDto {
    @Schema(description = "Order identifier")
    private Long id;

    @Schema(description = "Internal order code")
    private String orderCode;

    @Schema(description = "Originating request identifier")
    private Long requestId;

    @Schema(description = "Originating request code")
    private String requestCode;

    @Schema(description = "Order type")
    private OrderType type;

    @Schema(description = "Internal order status")
    private OrderStatus status;

    @Schema(description = "Nhanh sync status")
    private OrderSyncStatus syncStatus;

    @Schema(description = "Total order amount")
    private BigDecimal totalAmount;

    @Schema(description = "Deposit amount")
    private BigDecimal depositAmount;

    @Schema(description = "Order description")
    private String description;

    @Schema(description = "Customer name")
    private String customerName;

    @Schema(description = "Customer mobile")
    private String customerMobile;

    @Schema(description = "Customer email")
    private String customerEmail;

    @Schema(description = "Customer address")
    private String customerAddress;

    @Schema(description = "Customer city/province")
    private String customerCityName;

    @Schema(description = "Customer district")
    private String customerDistrictName;

    @Schema(description = "Customer ward")
    private String customerWardName;

    @Schema(description = "Nhanh order id")
    private String nhanhOrderId;

    @Schema(description = "Nhanh order code")
    private String nhanhOrderCode;

    @Schema(description = "Last sync error")
    private String syncError;

    @Schema(description = "Order items")
    private List<OrderItemResponseDto> items;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp")
    private LocalDateTime updatedAt;
}
