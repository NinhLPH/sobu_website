package com.vn.sodu.nhanh.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.location.LocationDataUnavailableException;
import com.vn.sodu.nhanh.service.NhanhLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/locations")
public class NhanhLocationController {

    private final NhanhLocationService nhanhLocationService;
    private final NhanhProperties nhanhProperties;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<LocationTreeResponse>> getLocations() {
        try {
            LocationTreeResponse response = nhanhLocationService.getLocations();
            String message = response.isStale()
                    ? "Locations retrieved from stale cache"
                    : "Locations retrieved successfully";
            return ResponseEntity.ok(ApiResponseDTO.success(response, message));
        } catch (LocationDataUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(
                            HttpHeaders.RETRY_AFTER,
                            String.valueOf(nhanhProperties.getLocation().getRetryAfterSeconds()))
                    .body(ApiResponseDTO.<LocationTreeResponse>error(
                            ex.getMessage(),
                            "LOCATION_DATA_UNAVAILABLE",
                            HttpStatus.SERVICE_UNAVAILABLE.value()));
        }
    }
}
