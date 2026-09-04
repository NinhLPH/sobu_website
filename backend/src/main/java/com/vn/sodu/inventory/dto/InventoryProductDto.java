package com.vn.sodu.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryProductDto {

    private Long id;
    private Long productId;
    private Long externalId;
    private String name;
    private String code;
    private String sku;
    private String barcode;
    private String avatarImage;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private BigDecimal price;
    private BigDecimal retailPrice;
    private Double stockRemain;
    private Double stockAvailable;
    private Double reserved;
    private String status;
    private Boolean active;
    private LocalDateTime updatedAt;
}
