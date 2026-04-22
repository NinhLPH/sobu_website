package com.vn.sodu.product.category.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhanhCategoryDTO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Integer order;
    private String image;
    private String content;
    private Integer status;
}
