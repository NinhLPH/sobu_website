package com.vn.sodu.nhanh.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.nhanh.dto.ShippingQuoteDto;
import com.vn.sodu.nhanh.dto.ShippingQuoteRequestDto;
import com.vn.sodu.nhanh.service.NhanhShippingQuoteService;
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
public class NhanhShippingQuoteController {

    private final NhanhShippingQuoteService nhanhShippingQuoteService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<List<ShippingQuoteDto>>> quote(
            @RequestBody ShippingQuoteRequestDto request) {
        List<ShippingQuoteDto> quotes = nhanhShippingQuoteService.quote(request);
        return ResponseEntity.ok(ApiResponseDTO.success(quotes, "Shipping quotes retrieved successfully"));
    }
}
