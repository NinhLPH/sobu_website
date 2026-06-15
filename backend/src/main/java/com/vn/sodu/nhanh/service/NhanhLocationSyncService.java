package com.vn.sodu.nhanh.service;

import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.dto.LocationCityDTO;
import com.vn.sodu.nhanh.dto.LocationDistrictDTO;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.dto.LocationWardDTO;
import com.vn.sodu.nhanh.dto.NhanhLocationItemDTO;
import com.vn.sodu.nhanh.location.NhanhLocationExtendedLockException;
import com.vn.sodu.nhanh.location.NhanhLocationRateLimiter;
import com.vn.sodu.nhanh.location.NhanhLocationSleeper;
import com.vn.sodu.nhanh.location.NhanhLocationSnapshotStore;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NhanhLocationSyncService {

    private static final String TYPE_CITY = "CITY";
    private static final String TYPE_DISTRICT = "DISTRICT";
    private static final String TYPE_WARD = "WARD";
    private static final ParameterizedTypeReference<NhanhResponse<List<NhanhLocationItemDTO>>>
            LOCATION_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
            };

    private final NhanhClient nhanhClient;
    private final NhanhService nhanhService;
    private final NhanhProperties nhanhProperties;
    private final NhanhLocationMapper locationMapper;
    private final NhanhLocationSnapshotStore snapshotStore;
    private final NhanhLocationRateLimiter rateLimiter;
    private final NhanhLocationSleeper sleeper;
    private final Clock clock;

    @Autowired
    public NhanhLocationSyncService(
            NhanhClient nhanhClient,
            NhanhService nhanhService,
            NhanhProperties nhanhProperties,
            NhanhLocationMapper locationMapper,
            NhanhLocationSnapshotStore snapshotStore,
            NhanhLocationRateLimiter rateLimiter,
            NhanhLocationSleeper sleeper) {
        this(
                nhanhClient,
                nhanhService,
                nhanhProperties,
                locationMapper,
                snapshotStore,
                rateLimiter,
                sleeper,
                Clock.systemUTC());
    }

    NhanhLocationSyncService(
            NhanhClient nhanhClient,
            NhanhService nhanhService,
            NhanhProperties nhanhProperties,
            NhanhLocationMapper locationMapper,
            NhanhLocationSnapshotStore snapshotStore,
            NhanhLocationRateLimiter rateLimiter,
            NhanhLocationSleeper sleeper,
            Clock clock) {
        this.nhanhClient = nhanhClient;
        this.nhanhService = nhanhService;
        this.nhanhProperties = nhanhProperties;
        this.locationMapper = locationMapper;
        this.snapshotStore = snapshotStore;
        this.rateLimiter = rateLimiter;
        this.sleeper = sleeper;
        this.clock = clock;
    }

    public void synchronize() {
        Instant startedAt = clock.instant();
        String accessToken = nhanhService.getValidAccessToken();
        List<NhanhLocationItemDTO> cityItems = validItems(
                fetchLocations(accessToken, TYPE_CITY, null));
        if (cityItems.isEmpty()) {
            throw new ExternalServiceException(
                    "Nhanh location sync returned no cities; existing snapshot is unchanged");
        }

        List<CityDiscovery> discovered = new ArrayList<>();
        int districtCount = 0;
        for (NhanhLocationItemDTO city : cityItems) {
            List<NhanhLocationItemDTO> districts = validItems(
                    fetchLocations(accessToken, TYPE_DISTRICT, city.getId()));
            discovered.add(new CityDiscovery(city, districts));
            districtCount += districts.size();
        }

        List<List<CityDiscovery>> chunks = buildBalancedChunks(discovered);
        long estimatedRequestCount = 1L + cityItems.size() + districtCount;
        long estimatedMs = estimatedRequestCount * nhanhProperties.getLocation().getRequestIntervalMs()
                + Math.max(0, chunks.size() - 1L)
                * nhanhProperties.getLocation().getInterChunkSleepMs();
        log.info(
                "Nhanh location discovery complete. cities={}, districts={}, totalRequests={}, estimatedDurationSeconds={}",
                cityItems.size(),
                districtCount,
                estimatedRequestCount,
                Duration.ofMillis(estimatedMs).toSeconds());

        Map<Long, LocationCityDTO> completedCities = new LinkedHashMap<>();
        int wardCount = 0;
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            Instant chunkStartedAt = clock.instant();
            List<CityDiscovery> chunk = chunks.get(chunkIndex);
            int chunkWardCount = 0;

            for (CityDiscovery city : chunk) {
                List<LocationDistrictDTO> completedDistricts = new ArrayList<>();
                for (NhanhLocationItemDTO district : city.districts()) {
                    List<LocationWardDTO> wards = validItems(
                            fetchLocations(accessToken, TYPE_WARD, district.getId()))
                            .stream()
                            .map(locationMapper::toWard)
                            .toList();
                    chunkWardCount += wards.size();
                    completedDistricts.add(locationMapper.toDistrict(district, wards));
                }
                completedCities.put(
                        city.city().getId(),
                        locationMapper.toCity(city.city(), completedDistricts));
            }
            wardCount += chunkWardCount;
            log.info(
                    "Nhanh location chunk complete. chunk={}/{}, cities={}, districtRequests={}, wards={}, elapsedSeconds={}",
                    chunkIndex + 1,
                    chunks.size(),
                    chunk.size(),
                    chunk.stream().mapToInt(item -> item.districts().size()).sum(),
                    chunkWardCount,
                    Duration.between(chunkStartedAt, clock.instant()).toSeconds());

            if (chunkIndex + 1 < chunks.size()) {
                sleeper.sleep(Duration.ofMillis(
                        Math.max(
                                0,
                                nhanhProperties.getLocation().getInterChunkSleepMs())));
                rateLimiter.resetAfterPause();
            }
        }

        List<LocationCityDTO> cities = cityItems.stream()
                .map(item -> completedCities.get(item.getId()))
                .toList();
        Instant syncedAt = clock.instant();
        Instant expiresAt = syncedAt.plus(Duration.ofHours(
                nhanhProperties.getLocation().getCacheTtlHours()));
        LocationTreeResponse tree = locationMapper.toTree(
                nhanhProperties.getLocation().getVersion(),
                syncedAt,
                expiresAt,
                false,
                cities);
        snapshotStore.save(tree, cities.size(), districtCount, wardCount);

        log.info(
                "Nhanh location sync complete. cities={}, districts={}, wards={}, elapsedSeconds={}",
                cities.size(),
                districtCount,
                wardCount,
                Duration.between(startedAt, clock.instant()).toSeconds());
    }

    List<List<CityDiscovery>> buildBalancedChunks(List<CityDiscovery> cities) {
        if (cities.isEmpty()) {
            return List.of();
        }
        int chunkSize = Math.max(1, nhanhProperties.getLocation().getChunkSize());
        int chunkCount = (cities.size() + chunkSize - 1) / chunkSize;
        List<List<CityDiscovery>> chunks = new ArrayList<>();
        int[] weights = new int[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            chunks.add(new ArrayList<>());
        }

        List<CityDiscovery> weighted = new ArrayList<>(cities);
        weighted.sort(Comparator
                .comparingInt((CityDiscovery item) -> item.districts().size())
                .reversed()
                .thenComparing(item -> item.city().getId()));

        for (CityDiscovery city : weighted) {
            int target = -1;
            for (int i = 0; i < chunks.size(); i++) {
                if (chunks.get(i).size() >= chunkSize) {
                    continue;
                }
                if (target < 0 || weights[i] < weights[target]) {
                    target = i;
                }
            }
            chunks.get(target).add(city);
            weights[target] += city.districts().size();
        }
        return chunks;
    }

    private List<NhanhLocationItemDTO> fetchLocations(
            String accessToken,
            String type,
            Long parentId) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("locationVersion", nhanhProperties.getLocation().getVersion());
        filters.put("type", type);
        if (parentId != null) {
            filters.put("parentId", parentId);
        }

        int maxAttempts = Math.max(
                1,
                nhanhProperties.getLocation().getMaxAttemptsPerRequest());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            rateLimiter.acquire();
            try {
                NhanhResponse<List<NhanhLocationItemDTO>> response = nhanhClient.postOnce(
                        nhanhProperties.getLocation().getPath(),
                        accessToken,
                        Map.of("filters", filters),
                        LOCATION_RESPONSE_TYPE);
                if (response == null || response.getData() == null) {
                    throw new ExternalServiceException("Nhanh location response data is null");
                }
                return response.getData();
            } catch (NhanhApiException ex) {
                if (ex.isRateLimited() && ex.getUnlockedAt() != null) {
                    Instant retryAt = ex.getUnlockedAt().plusSeconds(1);
                    Duration wait = Duration.between(clock.instant(), retryAt);
                    if (wait.toMillis() > nhanhProperties.getLocation().getExtendedLockThresholdMs()) {
                        throw new NhanhLocationExtendedLockException(retryAt);
                    }
                    if (attempt == maxAttempts) {
                        throw ex;
                    }
                    sleeper.sleep(wait);
                    rateLimiter.resetAfterPause();
                    continue;
                }
                if (!ex.isRetryable() || attempt == maxAttempts) {
                    throw ex;
                }
                sleepForRetry(attempt);
            }
        }
        throw new ExternalServiceException("Nhanh location request exhausted all attempts");
    }

    private void sleepForRetry(int failedAttempt) {
        List<Long> backoffs = nhanhProperties.getLocation().getRetryBackoffSeconds();
        long seconds = 1;
        if (backoffs != null && !backoffs.isEmpty()) {
            int index = Math.min(failedAttempt - 1, backoffs.size() - 1);
            Long configuredSeconds = backoffs.get(index);
            seconds = configuredSeconds == null ? 1 : configuredSeconds;
        }
        sleeper.sleep(Duration.ofSeconds(Math.max(0, seconds)));
        rateLimiter.resetAfterPause();
    }

    private List<NhanhLocationItemDTO> validItems(List<NhanhLocationItemDTO> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && item.getId() != null)
                .toList();
    }

    record CityDiscovery(
            NhanhLocationItemDTO city,
            List<NhanhLocationItemDTO> districts) {
    }
}
