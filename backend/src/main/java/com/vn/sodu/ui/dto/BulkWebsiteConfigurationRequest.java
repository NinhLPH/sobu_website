package com.vn.sodu.ui.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkWebsiteConfigurationRequest {
    private String key;
    private String value;
}
