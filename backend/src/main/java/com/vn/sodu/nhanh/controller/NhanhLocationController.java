package com.vn.sodu.nhanh.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.service.NhanhLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/locations")
public class NhanhLocationController {

    private final NhanhLocationService nhanhLocationService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<LocationTreeResponse>> getLocations() {
        LocationTreeResponse response = nhanhLocationService.getLocations();
        String message = response.isStale()
                ? "Locations retrieved from stale cache"
                : "Locations retrieved successfully";
        return ResponseEntity.ok(ApiResponseDTO.success(response, message));
    }
}
