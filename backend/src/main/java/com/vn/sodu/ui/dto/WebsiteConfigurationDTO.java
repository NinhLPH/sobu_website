package com.vn.sodu.ui.dto;

import com.vn.sodu.ui.WebsiteConfiguration;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class WebsiteConfigurationDTO {
    private Long id;
    private String key;
    private String value;
    private WebsiteConfiguration.ConfigType type;
    private String groupName;
    private String description;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
