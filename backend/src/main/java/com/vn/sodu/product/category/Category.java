package com.vn.sodu.product.category;

import com.vn.sodu.utilites.SlugUtils;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_category_parent", columnList = "parentId"),
                @Index(name = "idx_category_slug", columnList = "slug")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(name = "parent_id")
    private Long parentId;

    private String code;

    private String name;

    @Column(name = "slug")
    private String slug;

    @Column(name = "seo_title")
    private String seoTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Column(name = "canonical_url")
    private String canonicalUrl;

    @Column(name = "sort_order")
    private Integer order;

    private String image;

    @Column(name = "image_alt")
    private String imageAlt;

    @Column(name = "intro_content", columnDefinition = "TEXT")
    private String introContent;

    @Column(name = "footer_content", columnDefinition = "TEXT")
    private String footerContent;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer status;

    // ===== RELATION (READ ONLY) =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Category parent;

    @PrePersist
    public void prePersist() {
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.toSlug(name);
        }
        if (imageAlt == null || imageAlt.isBlank()) {
            imageAlt = name;
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.toSlug(name);
        }
    }
}