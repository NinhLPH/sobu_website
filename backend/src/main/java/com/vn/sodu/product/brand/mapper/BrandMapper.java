package com.vn.sodu.product.brand.mapper;

import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.dto.BrandDTO;
import com.vn.sodu.product.brand.dto.BrandListItemDTO;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import com.vn.sodu.seo.dto.SeoMetadataDTO;
import com.vn.sodu.utilites.SlugUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class BrandMapper {

    public Brand toEntity(NhanhBrandDTO dto) {
        if (dto == null) {
            return null;
        }

        return Brand.builder()
                .id(dto.getId())
                .externalId(dto.getId())
                .parentId(dto.getParentId())
                .code(dto.getCode())
                .name(dto.getName())
                .slug(SlugUtils.toSlug(dto.getName()))
                .logoAlt(dto.getName())
                .status(dto.getStatus())
                .createdAt(toLocalDateTime(dto.getCreatedAt()))
                .build();
    }

    public BrandListItemDTO toListItem(Brand brand) {
        if (brand == null) {
            return null;
        }

        String slug = brand.getSlug() != null && !brand.getSlug().isBlank()
                ? brand.getSlug()
                : SlugUtils.toSlug(brand.getName());

        return BrandListItemDTO.builder()
                .id(brand.getId())
                .externalId(brand.getExternalId())
                .parentId(brand.getParentId())
                .code(brand.getCode())
                .name(brand.getName())
                .slug(slug)
                .logoUrl(brand.getLogoUrl())
                .logoAlt(brand.getLogoAlt() != null ? brand.getLogoAlt() : brand.getName())
                .description(brand.getDescription())
                .status(brand.getStatus())
                .build();
    }

    public BrandDTO toDTO(Brand brand) {
        if (brand == null) {
            return null;
        }

        String slug = brand.getSlug() != null && !brand.getSlug().isBlank()
                ? brand.getSlug()
                : SlugUtils.toSlug(brand.getName());

        String seoTitle = brand.getSeoTitle() != null && !brand.getSeoTitle().isBlank()
                ? brand.getSeoTitle()
                : brand.getName() + " | Sobu";

        String metaDesc = brand.getMetaDescription() != null && !brand.getMetaDescription().isBlank()
                ? brand.getMetaDescription()
                : (brand.getDescription() != null ? brand.getDescription() : brand.getName());

        SeoMetadataDTO seo = SeoMetadataDTO.builder()
                .seoTitle(seoTitle)
                .metaDescription(metaDesc)
                .robots("index, follow")
                .ogTitle(seoTitle)
                .ogDescription(metaDesc)
                .ogImage(brand.getLogoUrl())
                .ogType("website")
                .build();

        return BrandDTO.builder()
                .id(brand.getId())
                .externalId(brand.getExternalId())
                .parentId(brand.getParentId())
                .code(brand.getCode())
                .name(brand.getName())
                .slug(slug)
                .logoUrl(brand.getLogoUrl())
                .logoAlt(brand.getLogoAlt() != null ? brand.getLogoAlt() : brand.getName())
                .description(brand.getDescription())
                .status(brand.getStatus())
                .seo(seo)
                .build();
    }

    private LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }

        long normalized = timestamp;
        if (Math.abs(normalized) < 1_000_000_000_000L) {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(normalized), ZoneId.systemDefault());
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(normalized), ZoneId.systemDefault());
    }
}
