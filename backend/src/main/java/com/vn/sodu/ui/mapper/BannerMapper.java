package com.vn.sodu.ui.mapper;

import com.vn.sodu.ui.Banner;
import com.vn.sodu.ui.dto.BannerDTO;
import com.vn.sodu.ui.dto.CreateBannerRequest;
import com.vn.sodu.ui.dto.UpdateBannerRequest;
import org.springframework.stereotype.Component;

@Component
public class BannerMapper {

    public BannerDTO toDTO(Banner entity) {
        if (entity == null) {
            return null;
        }
        return BannerDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .imageUrl(entity.getImageUrl())
                .linkUrl(entity.getLinkUrl())
                .displayOrder(entity.getDisplayOrder())
                .position(entity.getPosition())
                .isActive(entity.getIsActive())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .deviceType(entity.getDeviceType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Banner toEntity(CreateBannerRequest request) {
        if (request == null) {
            return null;
        }
        return Banner.builder()
                .title(request.getTitle())
                .imageUrl(request.getImageUrl())
                .linkUrl(request.getLinkUrl())
                .displayOrder(request.getDisplayOrder())
                .position(request.getPosition())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .deviceType(request.getDeviceType())
                .isActive(true)
                .build();
    }

    public void updateEntity(Banner entity, UpdateBannerRequest request) {
        if (entity == null || request == null) {
            return;
        }
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getImageUrl() != null) {
            entity.setImageUrl(request.getImageUrl());
        }
        if (request.getLinkUrl() != null) {
            entity.setLinkUrl(request.getLinkUrl());
        }
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getPosition() != null) {
            entity.setPosition(request.getPosition());
        }
        if (request.getStartDate() != null) {
            entity.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            entity.setEndDate(request.getEndDate());
        }
        if (request.getDeviceType() != null) {
            entity.setDeviceType(request.getDeviceType());
        }
    }
}
