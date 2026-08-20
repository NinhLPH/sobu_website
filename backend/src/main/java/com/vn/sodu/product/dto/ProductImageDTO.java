package com.vn.sodu.product.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDTO {
    private Long id;
    private String url;
    private String altText;
    private String caption;
    private Integer width;
    private Integer height;
    private Integer sortOrder;
    private Boolean isAvatar;
}
