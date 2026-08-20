package com.vn.sodu.voucher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherApplyRequestDto {
    private String discountVoucherCode;
    private String itemVoucherCode;
    private String orderVoucherCode;
    private String shippingVoucherCode;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private List<VoucherCartItemDto> items;
    private String customerCityName;
    private String customerDistrictName;
    private String customerWardName;
    private Long customerCityId;
    private Long customerDistrictId;
    private Long customerWardId;
    @Builder.Default
    private Boolean autoApply = true;
}
