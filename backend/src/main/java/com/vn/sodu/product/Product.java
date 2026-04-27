package com.vn.sodu.product;

import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.category.Category;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products",
            indexes = {
                @Index(name = "idx_product_status_category", columnList = "status, categoryId"),
                @Index(name = "idx_product_brand", columnList = "brandId"),
               @Index(name = "idx_product_code", columnList = "code")
   }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private Long id;

    private Long parentId;

    private String code;

    private String barcode;

    private String name;

    private String otherName;

    private String status;

    // ===== CATEGORY =====
    private Long categoryId;
    private String categoryName;

    private Long internalCategoryId;
    private String internalCategoryName;

    // ===== BRAND =====
    @Column(name = "brandId")
    private Long brandId;

    private String brandName; // optional (cache để tránh join)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brandId", insertable = false, updatable = false)
    private Brand brand;

    // ===== TYPE =====
    private Long typeId;
    private String typeName;

    // ===== SUPPLIER =====
    private Long supplierId;
    private String supplierName;
    private String supplierPhone;

    // ===== PRICE =====
    private BigDecimal retailPrice;
    private BigDecimal importPrice;
    private BigDecimal wholesalePrice;
    private BigDecimal oldPrice;
    private BigDecimal avgCost;

    private Integer vat;

    // ===== IMAGE =====
    private String avatarImage;

    // ===== SHIPPING =====
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer weight;

    private String countryName;

    // ===== INVENTORY =====
    private Double stockRemain;
    private Double stockAvailable;

    // ===== CONTENT =====
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    // ===== TIME =====
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // fallback
    @Column(columnDefinition = "TEXT")
    private String rawData;

    // ===== RELATION =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoryId", insertable = false, updatable = false)
    private Category category;
}