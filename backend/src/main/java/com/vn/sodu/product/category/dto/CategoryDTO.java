package com.vn.sodu.product.category.dto;

import com.vn.sodu.seo.dto.SeoMetadataDTO;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private String slug;
    private Integer order;
    private String image;
    private String imageAlt;
    private String introContent;
    private String footerContent;
    private String content;
    private Integer status;
    private SeoMetadataDTO seo;
}
