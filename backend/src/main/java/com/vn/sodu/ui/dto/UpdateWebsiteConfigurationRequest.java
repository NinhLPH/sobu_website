package com.vn.sodu.ui.dto;

import com.vn.sodu.ui.WebsiteConfiguration;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UpdateWebsiteConfigurationRequest {
    private String key;
    private String value;
    private WebsiteConfiguration.ConfigType type;
    private String groupName;
    private String description;
    private Boolean isPublic;
}
