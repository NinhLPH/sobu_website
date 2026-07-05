package com.vn.sodu.nhanh.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.nhanh.dto.CarrierConfigRequestDto;
import com.vn.sodu.nhanh.dto.CarrierConfigResponseDto;
import com.vn.sodu.nhanh.service.NhanhShippingQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shipping")
@RequiredArgsConstructor
@Tag(name = "Admin Nhanh Shipping", description = "Admin endpoints for managing Nhanh shipping carrier configurations")
public class AdminNhanhShippingController {

    private final NhanhShippingQuoteService nhanhShippingQuoteService;

    @GetMapping("/carriers")
    @Operation(
            summary = "Get carriers from Nhanh",
            description = "Fetches the list of all active shipping carriers connected to the Nhanh POS account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carriers retrieved successfully from Nhanh"),
            @ApiResponse(responseCode = "502", description = "Failed to fetch carriers from Nhanh API")
    })
    public ResponseEntity<ApiResponseDTO<com.fasterxml.jackson.databind.JsonNode>> getCarriers() {
        com.fasterxml.jackson.databind.JsonNode carriers = nhanhShippingQuoteService.getCarriers();
        return ResponseEntity.ok(ApiResponseDTO.success(carriers, "Carriers retrieved successfully from Nhanh"));
    }

    @GetMapping("/carrier-config")
    @Operation(
            summary = "Get shipping carrier configuration",
            description = "Retrieves the current default and express shipping carriers and services configured in the system."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrier configuration retrieved successfully")
    })
    public ResponseEntity<ApiResponseDTO<CarrierConfigResponseDto>> getCarrierConfig() {
        CarrierConfigResponseDto config = nhanhShippingQuoteService.getCarrierConfig();
        return ResponseEntity.ok(ApiResponseDTO.success(config, "Carrier configuration retrieved successfully"));
    }

    @PutMapping("/carrier-config")
    @Operation(
            summary = "Update shipping carrier configuration",
            description = "Saves new standard, express, and fallback shipping carrier IDs and service codes in the database."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrier configuration updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid configuration payload"),
            @ApiResponse(responseCode = "502", description = "Nhanh integration not found — authenticate first")
    })
    public ResponseEntity<ApiResponseDTO<Void>> updateCarrierConfig(
            @Valid @RequestBody CarrierConfigRequestDto request) {
        nhanhShippingQuoteService.saveCarrierConfig(request);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Carrier configuration updated successfully"));
    }
}