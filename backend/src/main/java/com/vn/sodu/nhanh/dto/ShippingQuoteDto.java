package com.vn.sodu.nhanh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Shipping fee quote returned from Nhanh API")
public class ShippingQuoteDto {
    @Schema(description = "Carrier ID", example = "22151")
    private Long carrierId;

    @Schema(description = "Carrier display name", example = "Viettel Post")
    private String carrierName;

    @Schema(description = "Carrier service ID", example = "1")
    private Long carrierServiceId;

    @Schema(description = "Carrier service name", example = "VCN")
    private String carrierServiceName;

    @Schema(description = "Actual shipping fee", example = "35000.00")
    private BigDecimal shipFee;

    @Schema(description = "Customer-facing shipping fee (may differ from shipFee)", example = "35000.00")
    private BigDecimal customerShipFee;

    @Schema(description = "Estimated delivery time", example = "2-3 ngày")
    private String deliveryTime;

    @Schema(description = "Additional description", example = "Giao hàng tiết kiệm")
    private String description;
}
