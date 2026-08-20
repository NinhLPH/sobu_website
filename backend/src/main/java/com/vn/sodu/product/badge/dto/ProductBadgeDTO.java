package com.vn.sodu.product.badge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBadgeDTO {

    private Long id;
    private String name;
    private String color;
    private String textColor;
    private Integer status;
    private LocalDateTime createdAt;
}