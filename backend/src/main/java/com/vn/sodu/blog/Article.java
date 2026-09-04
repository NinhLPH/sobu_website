package com.vn.sodu.blog;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "articles",
        indexes = {
                @Index(name = "idx_article_slug", columnList = "slug"),
                @Index(name = "idx_article_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    private String seoTitle;

    @Column(columnDefinition = "TEXT")
    private String metaDescription;

    private String canonicalUrl;

    private String thumbnailUrl;

    private String thumbnailAlt;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private String authorName;

    private String category;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PUBLISHED"; // DRAFT, PUBLISHED

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (publishedAt == null && "PUBLISHED".equalsIgnoreCase(status)) {
            publishedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
