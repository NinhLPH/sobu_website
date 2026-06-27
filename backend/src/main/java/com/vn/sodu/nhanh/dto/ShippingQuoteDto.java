package com.vn.sodu.nhanh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingQuoteDto {
    private Long carrierId;
    private String carrierName;
    private Long carrierServiceId;
    private String carrierServiceName;
    private BigDecimal shipFee;
    private BigDecimal customerShipFee;
    private String deliveryTime;
    private String description;
}
