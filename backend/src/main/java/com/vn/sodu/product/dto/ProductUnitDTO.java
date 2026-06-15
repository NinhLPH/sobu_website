package com.vn.sodu.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductUnitDTO {

    private Long id;
    private String name;
    private Integer quantity;
    private BigDecimal price;
}

