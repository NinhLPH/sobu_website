package com.vn.sodu.nhanh.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.nhanh.dto.ShippingQuoteDto;
import com.vn.sodu.nhanh.dto.ShippingQuoteRequestDto;
import com.vn.sodu.nhanh.service.NhanhShippingQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/shipping/quotes")
@Tag(name = "Shipping Quotes", description = "Public guest-facing shipping fee quote calculation endpoints")
public class NhanhShippingQuoteController {

    private final NhanhShippingQuoteService nhanhShippingQuoteService;

    @PostMapping
    @Operation(
            summary = "Calculate shipping fee quotes",
            description = "Queries Nhanh API and returns available standard and express shipping quotes based on customer address and cart subtotal."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipping quotes retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ShippingQuoteDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request — missing or invalid fields"),
            @ApiResponse(responseCode = "502", description = "Nhanh API failure — no shipping options available")
    })
    public ResponseEntity<ApiResponseDTO<List<ShippingQuoteDto>>> quote(
            @Valid @RequestBody ShippingQuoteRequestDto request) {
        List<ShippingQuoteDto> quotes = nhanhShippingQuoteService.quote(request);
        return ResponseEntity.ok(ApiResponseDTO.success(quotes, "Shipping quotes retrieved successfully"));
    }
}