package com.vn.sodu.product.brand.dto;

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
public class NhanhBrandDTO {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Integer status;
    private Long createdAt;
}
