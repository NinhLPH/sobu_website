package com.vn.sodu.order.dtos;

import com.vn.sodu.order.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponseDto {
    @Schema(description = "Order item identifier")
    private Long id;

    @Schema(description = "Nhanh product identifier")
    private String nhanhProductId;

    @Schema(description = "Item name")
    private String name;

    @Schema(description = "Item note")
    private String note;

    @Schema(description = "Item price")
    private BigDecimal price;

    @Schema(description = "Item discount")
    private BigDecimal discount;

    @Schema(description = "Item quantity")
    private Integer quantity;

    public static OrderItemResponseDto from(OrderItem item) {
        if (item == null) {
            return null;
        }
        return OrderItemResponseDto.builder()
                .id(item.getId())
                .nhanhProductId(item.getNhanhProductId())
                .name(item.getName())
                .note(item.getNote())
                .price(item.getPrice())
                .discount(item.getDiscount())
                .quantity(item.getQuantity())
                .build();
    }
}
