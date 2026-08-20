package com.vn.sodu.voucher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherApplyResponseDto {
    private boolean valid;
    private String discountVoucherCode;
    private String discountVoucherName;
    private String itemVoucherCode;
    private String itemVoucherName;
    private String orderVoucherCode;
    private String orderVoucherName;
    private String shippingVoucherCode;
    private String shippingVoucherName;

    @Builder.Default
    private BigDecimal itemDiscount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal orderDiscount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal subtotalDiscount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal shippingDiscount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    private BigDecimal originalSubtotal;
    private BigDecimal originalShippingFee;
    private BigDecimal finalSubtotal;
    private BigDecimal finalShippingFee;
    private BigDecimal finalTotal;

    @Builder.Default
    private List<AppliedVoucherDto> appliedVouchers = new ArrayList<>();

    private String message;
}
