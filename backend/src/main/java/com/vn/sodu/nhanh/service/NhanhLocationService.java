package com.vn.sodu.nhanh.service;

import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.dto.LocationCityDTO;
import com.vn.sodu.nhanh.dto.LocationDistrictDTO;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.dto.LocationWardDTO;
import com.vn.sodu.nhanh.dto.NhanhLocationItemDTO;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NhanhLocationService {

    private static final String TYPE_CITY = "CITY";
    private static final String TYPE_DISTRICT = "DISTRICT";
    private static final String TYPE_WARD = "WARD";
    private static final ParameterizedTypeReference<NhanhResponse<List<NhanhLocationItemDTO>>> LOCATION_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };

    private final NhanhClient nhanhClient;
    private final NhanhService nhanhService;
    private final NhanhProperties nhanhProperties;
    private final NhanhLocationMapper locationMapper;
    private final Clock clock;
    private final Object refreshLock = new Object();

    private volatile CachedLocationData cache;

    @Autowired
    public NhanhLocationService(
            NhanhClient nhanhClient,
            NhanhService nhanhService,
            NhanhProperties nhanhProperties,
            NhanhLocationMapper locationMapper) {
        this(nhanhClient, nhanhService, nhanhProperties, locationMapper, Clock.systemUTC());
    }

    NhanhLocationService(
            NhanhClient nhanhClient,
            NhanhService nhanhService,
            NhanhProperties nhanhProperties,
            NhanhLocationMapper locationMapper,
            Clock clock) {
        this.nhanhClient = nhanhClient;
        this.nhanhService = nhanhService;
        this.nhanhProperties = nhanhProperties;
        this.locationMapper = locationMapper;
        this.clock = clock;
    }

    public LocationTreeResponse getLocations() {
        Instant now = clock.instant();
        CachedLocationData current = cache;
        if (isCacheValid(current, now)) {
            return current.data();
        }

        synchronized (refreshLock) {
            now = clock.instant();
            current = cache;
            if (isCacheValid(current, now)) {
                return current.data();
            }

            try {
                return refreshCache(current, now).data();
            } catch (RuntimeException ex) {
                current = cache;
                if (current != null) {
                    log.warn(
                            "Nhanh location cache refresh failed. Returning stale cache cachedAt={}, expiresAt={}",
                            current.cachedAt(),
                            current.expiresAt(),
                            ex);
                    return locationMapper.withStale(current.data(), true);
                }

                log.error("Nhanh location cache refresh failed and no cache is available", ex);
                throw ex;
            }
        }
    }

    @Scheduled(fixedDelayString = "#{@nhanhProperties.location.cacheTtlHours * 60 * 60 * 1000}")
    public void refreshLocationsCache() {
        synchronized (refreshLock) {
            Instant now = clock.instant();
            CachedLocationData current = cache;
            try {
                refreshCache(current, now);
            } catch (RuntimeException ex) {
                log.warn(
                        "Scheduled Nhanh location cache refresh failed. Keeping existing cache cachedAt={}, expiresAt={}",
                        current == null ? null : current.cachedAt(),
                        current == null ? null : current.expiresAt(),
                        ex);
            }
        }
    }

    private boolean isCacheValid(CachedLocationData cached, Instant now) {
        return cached != null && now.isBefore(cached.expiresAt());
    }

    private CachedLocationData refreshCache(CachedLocationData previous, Instant cachedAt) {
        Instant previousExpiresAt = previous == null ? null : previous.expiresAt();
        log.info("Refreshing Nhanh location cache. Previous cache expired at {}", previousExpiresAt);

        String accessToken = nhanhService.getValidAccessToken();
        String locationVersion = nhanhProperties.getLocation().getVersion();

        List<NhanhLocationItemDTO> cityItems = fetchLocations(accessToken, TYPE_CITY, null);
        List<LocationCityDTO> cities = new ArrayList<>();
        int districtCount = 0;
        int wardCount = 0;

        // TODO: Future optimization: Parallel district/ward fetching using bounded
        // ExecutorService if refresh latency becomes an issue.
        for (NhanhLocationItemDTO cityItem : cityItems) {
            if (!hasId(cityItem)) {
                continue;
            }

            List<NhanhLocationItemDTO> districtItems = fetchLocations(accessToken, TYPE_DISTRICT, cityItem.getId());
            List<LocationDistrictDTO> districts = new ArrayList<>();

            for (NhanhLocationItemDTO districtItem : districtItems) {
                if (!hasId(districtItem)) {
                    continue;
                }

                List<NhanhLocationItemDTO> wardItems = fetchLocations(accessToken, TYPE_WARD, districtItem.getId());
                List<LocationWardDTO> wards = wardItems.stream()
                        .filter(this::hasId)
                        .map(locationMapper::toWard)
                        .toList();

                wardCount += wards.size();
                districts.add(locationMapper.toDistrict(districtItem, wards));
            }

            districtCount += districts.size();
            cities.add(locationMapper.toCity(cityItem, districts));
        }

        Instant expiresAt = cachedAt.plus(Duration.ofHours(nhanhProperties.getLocation().getCacheTtlHours()));
        LocationTreeResponse data = locationMapper.toTree(locationVersion, cachedAt, expiresAt, false, cities);
        CachedLocationData refreshed = new CachedLocationData(data, cachedAt, expiresAt);
        cache = refreshed;

        log.info(
                "Nhanh location cache refreshed. cities={}, districts={}, wards={}, cachedAt={}, expiresAt={}",
                cities.size(),
                districtCount,
                wardCount,
                cachedAt,
                expiresAt);
        return refreshed;
    }

    private List<NhanhLocationItemDTO> fetchLocations(String accessToken, String type, Long parentId) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("locationVersion", nhanhProperties.getLocation().getVersion());
        filters.put("type", type);
        if (parentId != null) {
            filters.put("parentId", parentId);
        }

        NhanhResponse<List<NhanhLocationItemDTO>> response = nhanhClient.postWithBearerAuthorization(
                nhanhProperties.getLocation().getPath(),
                accessToken,
                Map.of("filters", filters),
                LOCATION_RESPONSE_TYPE);

        if (response == null) {
            throw new ExternalServiceException("Nhanh location response is null");
        }
        if (response.getCode() != 1) {
            throw new ExternalServiceException(errorMessage(response));
        }
        if (response.getData() == null) {
            throw new ExternalServiceException("Nhanh location response data is null");
        }
        return response.getData();
    }

    private String errorMessage(NhanhResponse<?> response) {
        if (response.getMessages() == null || response.getMessages().isEmpty()) {
            return "Nhanh location API returned a non-success response";
        }
        return String.join("; ", response.getMessages());
    }

    private boolean hasId(NhanhLocationItemDTO item) {
        return item != null && item.getId() != null;
    }

    public record CachedLocationData(
            LocationTreeResponse data,
            Instant cachedAt,
            Instant expiresAt) {
    }
}
