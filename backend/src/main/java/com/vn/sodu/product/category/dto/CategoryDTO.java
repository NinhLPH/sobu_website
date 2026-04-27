package com.vn.sodu.product.category.dto;

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
    private Integer order;
    private String image;
    private String content;
    private Integer status;
}
