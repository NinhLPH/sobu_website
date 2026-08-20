package com.vn.sodu.seo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeoMetadataDTO {
    private String seoTitle;
    private String metaDescription;
    private String canonicalUrl;
    private String robots;
    private String ogTitle;
    private String ogDescription;
    private String ogImage;
    private String ogType;
}
