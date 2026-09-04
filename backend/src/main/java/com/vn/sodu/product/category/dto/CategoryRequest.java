package com.vn.sodu.product.category.dto;

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
public class CategoryRequest {

    private String code;
    private String name;
    private Long parentId;
    private Integer order;
    private String image;
    private String content;
    private Integer status;
}