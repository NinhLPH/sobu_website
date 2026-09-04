package com.vn.sodu.inventory.dto;

import com.vn.sodu.inventory.InventoryAdjustmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustmentDto {

    private Long id;
    private Long productId;
    private InventoryAdjustmentType type;
    private Double quantityDelta;
    private Double balanceAfter;
    private Long orderId;
    private String orderCode;
    private String note;
    private String actor;
    private LocalDateTime createdAt;
}