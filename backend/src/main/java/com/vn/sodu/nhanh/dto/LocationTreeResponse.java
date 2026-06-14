package com.vn.sodu.nhanh.dto;

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
public class LocationTreeResponse {
    private String provider;
    private String locationVersion;
    private Instant cachedAt;
    private Instant expiresAt;
    private boolean stale;
    private List<LocationCityDTO> cities;
}
