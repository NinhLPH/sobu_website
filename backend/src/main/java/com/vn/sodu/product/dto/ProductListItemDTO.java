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
    private Long externalId;
    private String name;
    private String slug;
    private String code;
    private String sku;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private BigDecimal salePrice;
    private String currency;
    private String status;
    private String conditionType;
    private String availability;
    private String avatarImage;
    private String avatarAltText;
    private Long brandId;
    private String brandName;
    private String brandSlug;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private Double stockAvailable;
    private Double averageRating;
    private Long reviewsCount;
    private Boolean active;
    private Long badgeId;
    private String badgeName;
    private String badgeColor;
    private String badgeTextColor;
}
