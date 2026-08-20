package com.vn.sodu.voucher.dto;

import com.vn.sodu.voucher.VoucherSlot;
import com.vn.sodu.voucher.VoucherType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppliedVoucherDto {
    private Long voucherId;
    private String code;
    private String name;
    private VoucherSlot slot;
    private VoucherType type;
    private BigDecimal discountAmount;
    private Boolean autoApplied;
}
