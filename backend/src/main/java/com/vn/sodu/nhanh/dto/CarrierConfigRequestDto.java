package com.vn.sodu.nhanh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload to update shipping carrier configuration")
public class CarrierConfigRequestDto {
    @Schema(description = "Default carrier ID", example = "22151")
    private Long carrierId;

    @Schema(description = "Standard delivery service code", example = "VCN")
    private String standardService;

    @Schema(description = "Express delivery service code", example = "VHT")
    private String expressService;

    @Schema(description = "Express carrier ID", example = "22384")
    private Long expressCarrierId;

    @Schema(description = "Fallback carrier ID for express when express service fails", example = "22384")
    private Long expressFallbackId;
}