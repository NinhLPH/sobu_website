package com.vn.sodu.product;

import com.vn.sodu.product.badge.ProductBadge;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.utilites.SlugUtils;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "external_id")
       },
       indexes = {
           @Index(name = "idx_product_status_category", columnList = "status, categoryId"),
           @Index(name = "idx_product_brand", columnList = "brandId"),
           @Index(name = "idx_product_code", columnList = "code"),
           @Index(name = "idx_product_slug", columnList = "slug")
       }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // external id from Nhanh API (nhanh_id). Nullable; used for upsert mapping.
    @Column(name = "external_id", unique = true)
    private Long externalId;

    private Long parentId;

    private String code;

    private String barcode;

    private String name;

    private String otherName;

    private String status;

    // ===== SEO FIELDS =====
    @Column(name = "slug")
    private String slug;

    @Column(name = "seo_title")
    private String seoTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "h1_title")
    private String h1Title;

    @Column(name = "canonical_url")
    private String canonicalUrl;

    @Column(name = "condition_type")
    @Builder.Default
    private String conditionType = "NEW"; // NEW, USED, REFURBISHED

    @Column(name = "availability")
    @Builder.Default
    private String availability = "IN_STOCK"; // IN_STOCK, OUT_OF_STOCK, PREORDER

    @Column(name = "preorder_expected_date")
    private LocalDate preorderExpectedDate;

    @Column(name = "currency")
    @Builder.Default
    private String currency = "VND";

    @Column(name = "sale_price")
    private BigDecimal salePrice;

    @Column(name = "sale_valid_from")
    private LocalDateTime saleValidFrom;

    @Column(name = "sale_valid_through")
    private LocalDateTime saleValidThrough;

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

    // ===== BADGE =====
    private Long badgeId;

    private String badgeName;

    private String badgeColor;

    private String badgeTextColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badgeId", insertable = false, updatable = false)
    private ProductBadge badge;

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

    @Column(nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean active = true;

    // fallback
    @Column(columnDefinition = "TEXT")
    private String rawData;

    // ===== RELATION =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoryId", insertable = false, updatable = false)
    private Category category;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.toSlug(name);
        }
        if (conditionType == null) {
            conditionType = "NEW";
        }
        if (availability == null) {
            availability = (stockAvailable != null && stockAvailable > 0) ? "IN_STOCK" : "OUT_OF_STOCK";
        }
        if (currency == null) {
            currency = "VND";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.toSlug(name);
        }
    }
}