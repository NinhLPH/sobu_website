package com.vn.sodu.ui.mapper;

import com.vn.sodu.ui.WebsiteConfiguration;
import com.vn.sodu.ui.dto.WebsiteConfigurationDTO;
import com.vn.sodu.ui.dto.WebsiteConfigurationRequest;
import org.springframework.stereotype.Component;

@Component
public class WebsiteConfigurationMapper {

    public WebsiteConfigurationDTO toDTO(WebsiteConfiguration entity) {
        if (entity == null) {
            return null;
        }
        return WebsiteConfigurationDTO.builder()
                .id(entity.getId())
                .key(entity.getKey())
                .value(entity.getValue())
                .type(entity.getType())
                .groupName(entity.getGroupName())
                .description(entity.getDescription())
                .isPublic(entity.getIsPublic())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public WebsiteConfiguration toEntity(WebsiteConfigurationRequest request) {
        if (request == null) {
            return null;
        }
        return WebsiteConfiguration.builder()
                .key(request.getKey())
                .value(request.getValue())
                .type(request.getType())
                .groupName(request.getGroupName())
                .description(request.getDescription())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .isActive(true)
                .build();
    }

    public void updateEntity(WebsiteConfiguration entity, WebsiteConfigurationRequest request) {
        if (entity == null || request == null) {
            return;
        }
        if (request.getKey() != null) {
            entity.setKey(request.getKey());
        }
        if (request.getValue() != null) {
            entity.setValue(request.getValue());
        }
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
        if (request.getGroupName() != null) {
            entity.setGroupName(request.getGroupName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getIsPublic() != null) {
            entity.setIsPublic(request.getIsPublic());
        }
    }
}
