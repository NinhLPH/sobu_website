package com.vn.sodu.product.dto;

import com.vn.sodu.seo.dto.SeoMetadataDTO;
import com.vn.sodu.voucher.dto.VoucherSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private String slug;
    private String otherName;
    private String code;
    private String sku;
    private String barcode;
    private String status;
    private String description;
    private String content;

    // Pricing & Sale
    private BigDecimal price;
    private BigDecimal wholesalePrice;
    private BigDecimal oldPrice;
    private BigDecimal salePrice;
    private LocalDateTime saleValidFrom;
    private LocalDateTime saleValidThrough;
    private String currency;
    private Integer vat;

    // Inventory & Condition
    private String conditionType;
    private String availability;
    private LocalDate preorderExpectedDate;
    private Double stockAvailable;
    private Double stockRemain;

    // Image & Brand & Category
    private String avatarImage;
    private String avatarAltText;
    private Long brandId;
    private String brandName;
    private String brandSlug;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    // Units & Attributes & Images
    private List<ProductUnitDTO> units;
    private List<ProductAttributeDTO> attributes;
    private List<String> images;
    private List<ProductImageDTO> imageDetails;

    // Rating & Time
    private Double averageRating;
    private Long reviewsCount;
    private LocalDateTime updatedAt;
    private Boolean active;

    // Badge
    private Long badgeId;
    private String badgeName;
    private String badgeColor;
    private String badgeTextColor;

    // SEO
    private String h1Title;
    private SeoMetadataDTO seo;

    // Voucher promotional data
    private VoucherSummaryDTO bestVoucher;
    private List<VoucherSummaryDTO> applicableVouchers;
}
