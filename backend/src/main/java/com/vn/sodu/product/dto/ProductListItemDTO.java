package com.vn.sodu.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListItemDTO {

    private Long id;
    private String name;
    private String code;
    private BigDecimal price;
    private String status;
    private String avatarImage;
    private String brandName;
    private String categoryName;
    private Double stockAvailable;
}

