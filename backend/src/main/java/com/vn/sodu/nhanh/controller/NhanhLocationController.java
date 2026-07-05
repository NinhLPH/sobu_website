package com.vn.sodu.nhanh.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.location.LocationDataUnavailableException;
import com.vn.sodu.nhanh.service.NhanhLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Locations", description = "Public guest-facing endpoints for location data (city/district/ward hierarchy)")
public class NhanhLocationController {

    private final NhanhLocationService nhanhLocationService;
    private final NhanhProperties nhanhProperties;

    @GetMapping
    @Operation(
            summary = "Get location tree",
            description = "Returns the full location hierarchy (cities → districts → wards) cached from Nhanh POS."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location data retrieved successfully (or from stale cache)",
                    content = @Content(schema = @Schema(implementation = LocationTreeResponse.class))),
            @ApiResponse(responseCode = "503", description = "Location data not yet available — initial sync in progress")
    })
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
