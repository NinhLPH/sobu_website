package com.vn.sodu.nhanh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request payload for calculating shipping fee quotes")
public class ShippingQuoteRequestDto {

    @Schema(description = "Detailed customer street address", example = "123 Nguyễn Huệ")
    private String customerAddress;

    @NotNull(message = "Customer city id is required")
    @Positive(message = "Customer city id must be positive")
    @Schema(description = "ID of the customer's city/province", example = "1")
    private Long customerCityId;

    @NotNull(message = "Customer district id is required")
    @Positive(message = "Customer district id must be positive")
    @Schema(description = "ID of the customer's district", example = "1")
    private Long customerDistrictId;

    @NotNull(message = "Customer ward id is required")
    @Positive(message = "Customer ward id must be positive")
    @Schema(description = "ID of the customer's ward/commune", example = "1")
    private Long customerWardId;

    @NotNull(message = "Cart subtotal is required")
    @DecimalMin(value = "0.00", message = "Cart subtotal must be greater than or equal to 0")
    @Schema(description = "Cart subtotal before shipping fee", example = "250000.00")
    private BigDecimal cartSubtotal;

    @DecimalMin(value = "0.00", message = "COD amount must be greater than or equal to 0")
    @Schema(description = "Cash on delivery amount (if applicable)", example = "0.00")
    private BigDecimal codAmount;

    @DecimalMin(value = "0.00", inclusive = false, message = "Shipping weight must be greater than 0")
    @Schema(description = "Shipping weight in grams", example = "1000")
    private BigDecimal shippingWeight;

    @Schema(description = "Specific carrier ID to query (optional)", example = "22151")
    private Long carrierId;

    @Schema(description = "Specific carrier service ID to query (optional)", example = "1")
    private Long carrierServiceId;
}
