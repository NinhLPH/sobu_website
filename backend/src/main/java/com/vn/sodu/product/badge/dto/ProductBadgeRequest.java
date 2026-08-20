package com.vn.sodu.product.badge.dto;

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
public class ProductBadgeRequest {

    private String name;
    private String color;
    private String textColor;
    private Integer status;
}