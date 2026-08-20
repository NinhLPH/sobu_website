package com.vn.sodu.product.brand;

import com.vn.sodu.utilites.SlugUtils;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "brands",
        indexes = {
                @Index(name = "idx_brand_parent", columnList = "parentId"),
                @Index(name = "idx_brand_slug", columnList = "slug")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true)
    private Long externalId;

    private String code;

    private String name;

    @Column(name = "slug")
    private String slug;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "logo_alt")
    private String logoAlt;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "seo_title")
    private String seoTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    private Integer status;

    private Long parentId;

    private LocalDateTime createdAt;

    // ===== RELATION (READ ONLY) =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentId", insertable = false, updatable = false)
    private Brand parent;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.toSlug(name);
        }
        if (logoAlt == null || logoAlt.isBlank()) {
            logoAlt = name;
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.toSlug(name);
        }
    }
}
