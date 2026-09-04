package com.vn.sodu.voucher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherCartItemDto {
    private Long productId;
    private Long categoryId;
    private Set<Long> categoryIds;
    private String name;
    private BigDecimal price;
    private Integer quantity;
}
