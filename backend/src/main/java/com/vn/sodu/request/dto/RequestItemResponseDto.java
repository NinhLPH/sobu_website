package com.vn.sodu.request.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RequestItemResponseDto {

    @Schema(description = "Item identifier")
    private Long id;

    @Schema(description = "Nhanh product id", example = "P001")
    private String nhanhProductId;

    @Schema(description = "Item name")
    private String name;

    @Schema(description = "Item note")
    private String note;

    @Schema(description = "Item metadata JSON")
    private String metadataJson;

    @Schema(description = "Item price")
    private BigDecimal price;

    @Schema(description = "Item quantity")
    private Integer quantity;
}
