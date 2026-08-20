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
    private String code;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private String status;
    private String avatarImage;
    private String brandName;
    private String categoryName;
    private Double stockAvailable;
    private Double averageRating;
    private Long reviewsCount;
    private Boolean active;
    private Long badgeId;
    private String badgeName;
    private String badgeColor;
    private String badgeTextColor;
}
