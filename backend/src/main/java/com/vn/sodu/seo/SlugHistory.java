package com.vn.sodu.seo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "slug_history",
        indexes = {
                @Index(name = "idx_slug_history_lookup", columnList = "entityType, oldSlug")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlugHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType; // PRODUCT, CATEGORY, BRAND, ARTICLE, PAGE

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "old_slug", nullable = false)
    private String oldSlug;

    @Column(name = "current_slug", nullable = false)
    private String currentSlug;

    @Column(name = "http_status")
    @Builder.Default
    private Integer httpStatus = 301;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
