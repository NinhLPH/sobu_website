package com.vn.sodu.product.dto;

import com.vn.sodu.product.ProductVideo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailDTO {

    private Long id;
    private Long externalId;
    private String name;
    private String otherName;
    private String code;
    private String barcode;
    private String status;
    private String description;
    private String content;
    private BigDecimal price;
    private BigDecimal wholesalePrice;
    private BigDecimal oldPrice;
    private Integer vat;
    private String avatarImage;
    private String brandName;
    private String categoryName;
    private Double stockAvailable;
    private Double stockRemain;
    private List<ProductUnitDTO> units;
    private List<ProductAttributeDTO> attributes;
    private List<String> images;
    private Double averageRating;
    private Long reviewsCount;
    private java.time.LocalDateTime updatedAt;

}

