package com.vn.sodu.blog;

import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.seo.SlugHistoryService;
import com.vn.sodu.seo.dto.SeoMetadataDTO;
import com.vn.sodu.utilites.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepo articleRepo;
    private final SlugHistoryService slugHistoryService;

    @Transactional(readOnly = true)
    public Page<ArticleDTO> getPublishedArticles(String category, Pageable pageable) {
        Page<Article> page = (category != null && !category.isBlank())
                ? articleRepo.findByStatusAndCategoryOrderByPublishedAtDesc("PUBLISHED", category, pageable)
                : articleRepo.findByStatusOrderByPublishedAtDesc("PUBLISHED", pageable);

        return page.map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public ArticleDetailDTO getPublishedArticleBySlug(String slug) {
        Article article = articleRepo.findBySlugAndStatus(slug, "PUBLISHED")
                .orElseGet(() -> {
                    // Check if slug is an old slug from history
                    String currentSlug = slugHistoryService.findCurrentSlug("ARTICLE", slug)
                            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với slug: " + slug));
                    return articleRepo.findBySlugAndStatus(currentSlug, "PUBLISHED")
                            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với slug: " + currentSlug));
                });

        return toDetailDTO(article);
    }

    @Transactional
    public ArticleDetailDTO createArticle(ArticleDetailDTO dto) {
        String slug = (dto.getSlug() != null && !dto.getSlug().isBlank())
                ? SlugUtils.toSlug(dto.getSlug())
                : SlugUtils.toSlug(dto.getTitle());

        slug = ensureUniqueSlug(slug, null);

        Article article = Article.builder()
                .title(dto.getTitle())
                .slug(slug)
                .seoTitle(dto.getSeoTitle() != null ? dto.getSeoTitle() : dto.getTitle())
                .metaDescription(dto.getMetaDescription() != null ? dto.getMetaDescription() : dto.getExcerpt())
                .canonicalUrl(dto.getCanonicalUrl())
                .thumbnailUrl(dto.getThumbnailUrl())
                .thumbnailAlt(dto.getThumbnailAlt() != null ? dto.getThumbnailAlt() : dto.getTitle())
                .excerpt(dto.getExcerpt())
                .content(dto.getContent())
                .authorName(dto.getAuthorName())
                .category(dto.getCategory())
                .status(dto.getStatus() != null ? dto.getStatus() : "PUBLISHED")
                .publishedAt(dto.getPublishedAt() != null ? dto.getPublishedAt() : LocalDateTime.now())
                .build();

        Article saved = articleRepo.save(article);
        return toDetailDTO(saved);
    }

    @Transactional
    public ArticleDetailDTO updateArticle(Long id, ArticleDetailDTO dto) {
        Article article = articleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + id));

        String oldSlug = article.getSlug();
        String newSlug = (dto.getSlug() != null && !dto.getSlug().isBlank())
                ? SlugUtils.toSlug(dto.getSlug())
                : SlugUtils.toSlug(dto.getTitle());

        if (!newSlug.equalsIgnoreCase(oldSlug)) {
            newSlug = ensureUniqueSlug(newSlug, id);
            slugHistoryService.recordSlugChange("ARTICLE", id, oldSlug, newSlug);
            article.setSlug(newSlug);
        }

        article.setTitle(dto.getTitle());
        article.setSeoTitle(dto.getSeoTitle() != null ? dto.getSeoTitle() : dto.getTitle());
        article.setMetaDescription(dto.getMetaDescription() != null ? dto.getMetaDescription() : dto.getExcerpt());
        article.setCanonicalUrl(dto.getCanonicalUrl());
        article.setThumbnailUrl(dto.getThumbnailUrl());
        article.setThumbnailAlt(dto.getThumbnailAlt() != null ? dto.getThumbnailAlt() : dto.getTitle());
        article.setExcerpt(dto.getExcerpt());
        article.setContent(dto.getContent());
        article.setAuthorName(dto.getAuthorName());
        article.setCategory(dto.getCategory());
        if (dto.getStatus() != null) {
            article.setStatus(dto.getStatus());
        }

        Article saved = articleRepo.save(article);
        return toDetailDTO(saved);
    }

    @Transactional
    public void deleteArticle(Long id) {
        if (!articleRepo.existsById(id)) {
            throw new NotFoundException("Không tìm thấy bài viết với ID: " + id);
        }
        articleRepo.deleteById(id);
    }

    private String ensureUniqueSlug(String baseSlug, Long excludeId) {
        String slug = baseSlug;
        int count = 1;
        while (true) {
            var existing = articleRepo.findBySlug(slug);
            if (existing.isEmpty() || (excludeId != null && existing.get().getId().equals(excludeId))) {
                return slug;
            }
            slug = baseSlug + "-" + (++count);
        }
    }

    private ArticleDTO toDTO(Article a) {
        return ArticleDTO.builder()
                .id(a.getId())
                .title(a.getTitle())
                .slug(a.getSlug())
                .thumbnailUrl(a.getThumbnailUrl())
                .thumbnailAlt(a.getThumbnailAlt())
                .excerpt(a.getExcerpt())
                .authorName(a.getAuthorName())
                .category(a.getCategory())
                .status(a.getStatus())
                .publishedAt(a.getPublishedAt())
                .updatedAt(a.getUpdatedAt())
                .seo(SeoMetadataDTO.builder()
                        .seoTitle(a.getSeoTitle() != null ? a.getSeoTitle() : a.getTitle())
                        .metaDescription(a.getMetaDescription() != null ? a.getMetaDescription() : a.getExcerpt())
                        .canonicalUrl(a.getCanonicalUrl())
                        .robots("index, follow")
                        .ogTitle(a.getSeoTitle() != null ? a.getSeoTitle() : a.getTitle())
                        .ogDescription(a.getMetaDescription() != null ? a.getMetaDescription() : a.getExcerpt())
                        .ogImage(a.getThumbnailUrl())
                        .ogType("article")
                        .build())
                .build();
    }

    private ArticleDetailDTO toDetailDTO(Article a) {
        return ArticleDetailDTO.builder()
                .id(a.getId())
                .title(a.getTitle())
                .slug(a.getSlug())
                .seoTitle(a.getSeoTitle())
                .metaDescription(a.getMetaDescription())
                .canonicalUrl(a.getCanonicalUrl())
                .thumbnailUrl(a.getThumbnailUrl())
                .thumbnailAlt(a.getThumbnailAlt())
                .excerpt(a.getExcerpt())
                .content(a.getContent())
                .authorName(a.getAuthorName())
                .category(a.getCategory())
                .status(a.getStatus())
                .publishedAt(a.getPublishedAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .seo(SeoMetadataDTO.builder()
                        .seoTitle(a.getSeoTitle() != null ? a.getSeoTitle() : a.getTitle())
                        .metaDescription(a.getMetaDescription() != null ? a.getMetaDescription() : a.getExcerpt())
                        .canonicalUrl(a.getCanonicalUrl())
                        .robots("index, follow")
                        .ogTitle(a.getSeoTitle() != null ? a.getSeoTitle() : a.getTitle())
                        .ogDescription(a.getMetaDescription() != null ? a.getMetaDescription() : a.getExcerpt())
                        .ogImage(a.getThumbnailUrl())
                        .ogType("article")
                        .build())
                .build();
    }
}
