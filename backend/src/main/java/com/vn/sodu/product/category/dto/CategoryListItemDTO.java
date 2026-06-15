package com.vn.sodu.product.category.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryListItemDTO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Integer order;
    private String image;
    private Integer status;
    private List<CategoryListItemDTO> children;
}
