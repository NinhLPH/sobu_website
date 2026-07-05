package com.vn.sodu.nhanh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Location tree response containing cities, districts, and wards")
public class LocationTreeResponse {
    @Schema(description = "Data provider name", example = "nhanh")
    private String provider;

    @Schema(description = "Location data version", example = "v1")
    private String locationVersion;

    @Schema(description = "When the data was cached", example = "2025-06-01T12:00:00Z")
    private Instant cachedAt;

    @Schema(description = "When the cache expires", example = "2025-06-02T12:00:00Z")
    private Instant expiresAt;

    @Schema(description = "Whether the data is stale (cache expired, serving cached data)")
    private boolean stale;

    @Schema(description = "List of cities/provinces")
    private List<LocationCityDTO> cities;
}
