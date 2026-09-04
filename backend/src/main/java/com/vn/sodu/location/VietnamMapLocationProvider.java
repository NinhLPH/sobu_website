package com.vn.sodu.location;

import com.vn.sodu.integration.IntegrationProperties;
import com.vn.sodu.location.VietnamMapApiClient.VietnamDistrict;
import com.vn.sodu.location.VietnamMapApiClient.VietnamProvince;
import com.vn.sodu.location.VietnamMapApiClient.VietnamProvinceDetail;
import com.vn.sodu.location.VietnamMapApiClient.VietnamWard;
import com.vn.sodu.nhanh.dto.LocationCityDTO;
import com.vn.sodu.nhanh.dto.LocationDistrictDTO;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.dto.LocationWardDTO;
import com.vn.sodu.nhanh.location.LocationDataUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Location provider for local mode: builds the city/district/ward tree from the
 * public Vietnam Map API and caches it in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VietnamMapLocationProvider {

    private static final String PROVIDER = "vietnam-map";
    private static final String LOCATION_VERSION = "v1";
    private static final int FETCH_PARALLELISM = 8;

    private final VietnamMapApiClient apiClient;
    private final VietnamLocationCacheStore cacheStore;
    private final IntegrationProperties integrationProperties;
    private final Clock clock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));

    public LocationTreeResponse getLocations() {
        Optional<LocationTreeResponse> cached = cacheStore.load();

        if (cached.isPresent() && !isExpired(cached.get())) {
            return cached.get();
        }

        try {
            LocationTreeResponse refreshed = refresh();
            return refreshed;
        } catch (RuntimeException ex) {
            if (cached.isPresent()) {
                log.warn("Vietnam location refresh failed; serving stale cache: {}", ex.getMessage());
                return cached.get();
            }
            throw new LocationDataUnavailableException();
        }
    }

    private boolean isExpired(LocationTreeResponse tree) {
        return tree.getExpiresAt() == null || !clock.instant().isBefore(tree.getExpiresAt());
    }

    private LocationTreeResponse refresh() {
        List<VietnamProvince> provinces = apiClient.fetchProvinces();
        if (provinces.isEmpty()) {
            throw new LocationDataUnavailableException();
        }

        List<LocationCityDTO> cities = fetchCities(provinces);
        if (cities.isEmpty()) {
            throw new LocationDataUnavailableException();
        }

        int districtCount = cities.stream()
                .mapToInt(city -> city.getDistricts() == null ? 0 : city.getDistricts().size())
                .sum();
        int wardCount = cities.stream()
                .flatMap(city -> city.getDistricts() == null ? java.util.stream.Stream.empty() : city.getDistricts().stream())
                .mapToInt(district -> district.getWards() == null ? 0 : district.getWards().size())
                .sum();

        Instant cachedAt = clock.instant();
        long ttlHours = integrationProperties.getNhanh().getVietnamMap().getCacheTtlHours();
        Instant expiresAt = cachedAt.plusSeconds(Math.max(1, ttlHours) * 3600);

        LocationTreeResponse tree = LocationTreeResponse.builder()
                .provider(PROVIDER)
                .locationVersion(LOCATION_VERSION)
                .cachedAt(cachedAt)
                .expiresAt(expiresAt)
                .stale(false)
                .cities(cities)
                .build();

        cacheStore.save(tree, cities.size(), districtCount, wardCount);
        log.info("Refreshed Vietnam location tree: cities={}, districts={}, wards={}",
                cities.size(), districtCount, wardCount);
        return tree;
    }

    private List<LocationCityDTO> fetchCities(List<VietnamProvince> provinces) {
        ExecutorService pool = Executors.newFixedThreadPool(FETCH_PARALLELISM);
        try {
            List<CompletableFuture<LocationCityDTO>> futures = provinces.stream()
                    .filter(Objects::nonNull)
                    .filter(p -> p.getCode() != null)
                    .map(province -> CompletableFuture
                            .supplyAsync(() -> buildCity(province), pool)
                            .exceptionally(ex -> {
                                log.warn("Failed to fetch Vietnam province {} ({}): {}",
                                        province.getCode(), province.getName(), ex.getMessage());
                                return null;
                            }))
                    .toList();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        } finally {
            pool.shutdown();
        }
    }

    private LocationCityDTO buildCity(VietnamProvince province) {
        VietnamProvinceDetail detail = apiClient.fetchProvinceDetail(province.getCode());
        if (detail == null) {
            return null;
        }

        List<LocationDistrictDTO> districts = new ArrayList<>();
        if (detail.getDistricts() != null) {
            for (VietnamDistrict district : detail.getDistricts()) {
                if (district == null || district.getCode() == null) {
                    continue;
                }
                List<LocationWardDTO> wards = new ArrayList<>();
                if (district.getWards() != null) {
                    for (VietnamWard ward : district.getWards()) {
                        if (ward == null || ward.getCode() == null) {
                            continue;
                        }
                        wards.add(LocationWardDTO.builder()
                                .wardId(ward.getCode())
                                .wardName(ward.getName())
                                .build());
                    }
                }
                districts.add(LocationDistrictDTO.builder()
                        .districtId(district.getCode())
                        .districtName(district.getName())
                        .wards(wards)
                        .build());
            }
        }

        return LocationCityDTO.builder()
                .cityId(detail.getCode())
                .cityName(detail.getName())
                .districts(districts)
                .build();
    }
}
