package com.vn.sodu.product.brand.dto;

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
public class BrandListItemDTO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Integer status;
}
