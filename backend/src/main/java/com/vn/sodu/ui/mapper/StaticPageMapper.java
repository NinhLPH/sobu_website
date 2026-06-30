package com.vn.sodu.ui.mapper;

import com.vn.sodu.ui.StaticPage;
import com.vn.sodu.ui.dto.StaticPageDTO;
import org.springframework.stereotype.Component;

@Component
public class StaticPageMapper {

    public StaticPageDTO toDTO(StaticPage entity) {
        if (entity == null) {
            return null;
        }
        return StaticPageDTO.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .title(entity.getTitle())
                .htmlContent(entity.getHtmlContent())
                .isPublished(entity.getIsPublished())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
