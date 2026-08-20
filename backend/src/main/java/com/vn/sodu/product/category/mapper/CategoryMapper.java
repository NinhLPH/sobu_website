package com.vn.sodu.product.category.mapper;

import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.dto.CategoryDTO;
import com.vn.sodu.product.category.dto.CategoryListItemDTO;
import com.vn.sodu.product.category.dto.NhanhCategoryDTO;
import com.vn.sodu.seo.dto.SeoMetadataDTO;
import com.vn.sodu.utilites.SlugUtils;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    /**
     * Convert Category entity to CategoryDTO
     */
    public CategoryDTO toDTO(Category category) {
        if (category == null) {
            return null;
        }

        String slug = category.getSlug() != null && !category.getSlug().isBlank()
                ? category.getSlug()
                : SlugUtils.toSlug(category.getName());

        String seoTitle = category.getSeoTitle() != null && !category.getSeoTitle().isBlank()
                ? category.getSeoTitle()
                : category.getName() + " | Sobu";

        String metaDesc = category.getMetaDescription() != null && !category.getMetaDescription().isBlank()
                ? category.getMetaDescription()
                : (category.getIntroContent() != null ? category.getIntroContent() : category.getName());

        SeoMetadataDTO seo = SeoMetadataDTO.builder()
                .seoTitle(seoTitle)
                .metaDescription(metaDesc)
                .canonicalUrl(category.getCanonicalUrl())
                .robots("index, follow")
                .ogTitle(seoTitle)
                .ogDescription(metaDesc)
                .ogImage(category.getImage())
                .ogType("website")
                .build();

        return CategoryDTO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .code(category.getCode())
                .name(category.getName())
                .slug(slug)
                .order(category.getOrder())
                .image(category.getImage())
                .imageAlt(category.getImageAlt() != null ? category.getImageAlt() : category.getName())
                .introContent(category.getIntroContent())
                .footerContent(category.getFooterContent())
                .content(category.getContent())
                .status(category.getStatus())
                .seo(seo)
                .build();
    }

    /**
     * Convert CategoryDTO to Category entity
     */
    public Category toEntity(CategoryDTO dto) {
        if (dto == null) {
            return null;
        }
        return Category.builder()
                .id(dto.getId())
                .parentId(dto.getParentId())
                .code(dto.getCode())
                .name(dto.getName())
                .slug(dto.getSlug() != null && !dto.getSlug().isBlank() ? dto.getSlug() : SlugUtils.toSlug(dto.getName()))
                .order(dto.getOrder())
                .image(dto.getImage())
                .imageAlt(dto.getImageAlt() != null ? dto.getImageAlt() : dto.getName())
                .introContent(dto.getIntroContent())
                .footerContent(dto.getFooterContent())
                .content(dto.getContent())
                .status(dto.getStatus())
                .seoTitle(dto.getSeo() != null ? dto.getSeo().getSeoTitle() : null)
                .metaDescription(dto.getSeo() != null ? dto.getSeo().getMetaDescription() : null)
                .canonicalUrl(dto.getSeo() != null ? dto.getSeo().getCanonicalUrl() : null)
                .build();
    }

    /**
     * Convert NhanhCategoryDTO to Category entity
     */
    public Category toEntity(NhanhCategoryDTO dto) {
        if (dto == null) {
            return null;
        }
        return Category.builder()
                .id(dto.getId())
                .externalId(dto.getId())
                .parentId(dto.getParentId())
                .code(dto.getCode())
                .name(dto.getName())
                .slug(SlugUtils.toSlug(dto.getName()))
                .order(dto.getOrder())
                .image(dto.getImage())
                .imageAlt(dto.getName())
                .content(dto.getContent())
                .status(dto.getStatus())
                .build();
    }

    /**
     * Convert Category entity to CategoryListDTO
     */
    public CategoryListItemDTO toListDTO(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryListItemDTO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .code(category.getCode())
                .name(category.getName())
                .slug(category.getSlug() != null && !category.getSlug().isBlank() ? category.getSlug() : SlugUtils.toSlug(category.getName()))
                .order(category.getOrder())
                .image(category.getImage())
                .imageAlt(category.getImageAlt() != null ? category.getImageAlt() : category.getName())
                .status(category.getStatus())
                .build();
    }
}
