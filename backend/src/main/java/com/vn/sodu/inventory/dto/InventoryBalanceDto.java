package com.vn.sodu.inventory.dto;

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
public class InventoryBalanceDto {

    private Long productId;
    private Double stockRemain;
    private Double stockAvailable;

    /** Sellable stock currently reserved by open orders (stockRemain - stockAvailable). */
    private Double reserved;
}