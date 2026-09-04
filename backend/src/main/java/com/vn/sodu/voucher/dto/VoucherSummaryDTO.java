package com.vn.sodu.voucher.dto;

import com.vn.sodu.voucher.GeoScope;
import com.vn.sodu.voucher.VoucherScope;
import com.vn.sodu.voucher.VoucherSlot;
import com.vn.sodu.voucher.VoucherType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherSummaryDTO {
    private Long id;
    private String code;
    private String name;
    private VoucherType type;
    private VoucherSlot slot;
    private VoucherScope scope;
    private GeoScope geoScope;
    private BigDecimal value;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private Boolean autoApply;
    private BigDecimal estimatedDiscount;
    private BigDecimal effectivePrice;
    private String badgeText;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
