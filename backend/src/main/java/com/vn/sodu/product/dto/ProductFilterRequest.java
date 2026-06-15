package com.vn.sodu.product.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest {

    private int page = 0;
    private int pageSize = 20;
    @JsonAlias("q")
    private String search;
    private String sortBy = "id";
    private String sortDirection = "DESC";

    private Long categoryId;
    private Long brandId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStock;
}

