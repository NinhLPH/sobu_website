package com.vn.sodu.inventory.dto;

import com.vn.sodu.inventory.InventoryAdjustmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustmentRequest {

    private InventoryAdjustmentType type;

    /** Positive quantity to apply. Direction is implied by the type. */
    private Double quantity;

    private String note;
}