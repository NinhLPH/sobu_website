package com.vn.sodu.nhanh.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class ShippingQuoteRequestDto {

    private String customerAddress;

    @NotNull(message = "Customer city id is required")
    @Positive(message = "Customer city id must be positive")
    private Long customerCityId;

    @NotNull(message = "Customer district id is required")
    @Positive(message = "Customer district id must be positive")
    private Long customerDistrictId;

    @NotNull(message = "Customer ward id is required")
    @Positive(message = "Customer ward id must be positive")
    private Long customerWardId;

    @NotNull(message = "Cart subtotal is required")
    @DecimalMin(value = "0.00", message = "Cart subtotal must be greater than or equal to 0")
    private BigDecimal cartSubtotal;

    @DecimalMin(value = "0.00", message = "COD amount must be greater than or equal to 0")
    private BigDecimal codAmount;

    @DecimalMin(value = "0.00", inclusive = false, message = "Shipping weight must be greater than 0")
    private BigDecimal shippingWeight;

    private Long carrierId;
    private Long carrierServiceId;
}
