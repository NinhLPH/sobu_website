package com.vn.sodu.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    private String code;
    private String barcode;
    private String name;
    private String otherName;
    private Long categoryId;
    private Long brandId;
    private Long badgeId;
    private BigDecimal retailPrice;
    private BigDecimal importPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal oldPrice;
    private Integer vat;
    private String avatarImage;
    private List<String> images;
    private List<ProductUnitRequest> units;
    private List<ProductAttributeRequest> attributes;
    private String description;
    private String content;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer weight;
    private String status;
    private Boolean active;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductUnitRequest {
        private String name;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal wholesalePrice;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAttributeRequest {
        private String name;
        private String value;
    }
}