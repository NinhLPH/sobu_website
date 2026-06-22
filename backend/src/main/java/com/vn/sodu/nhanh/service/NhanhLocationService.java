package com.vn.sodu.nhanh.service;

import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.location.LocationDataUnavailableException;
import com.vn.sodu.nhanh.location.NhanhLocationSnapshotStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NhanhLocationService {

    private final NhanhLocationSnapshotStore snapshotStore;

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

        int maxAttempts = Math.max(1, nhanhProperties.getLocation().getRateLimitMaxAttempts());
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            waitForRequestSlot();
            try {
                NhanhResponse<List<NhanhLocationItemDTO>> response = nhanhClient.post(
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
            } catch (NhanhRateLimitException ex) {
                if (attempt == maxAttempts) {
                    throw ex;
                }
                Duration wait = rateLimitWait(ex);
                log.warn(
                        "Nhanh location API rate limited. attempt={}/{}, lockedSeconds={}, unlockedAt={}, waitMs={}",
                        attempt,
                        maxAttempts,
                        ex.getLockedSeconds(),
                        ex.getUnlockedAt(),
                        wait.toMillis());
                sleep(wait);
            }
        }
        throw new ExternalServiceException("Nhanh location request exhausted all rate-limit retry attempts");
    }

    private void waitForRequestSlot() {
        sleep(Duration.ofMillis(Math.max(0, nhanhProperties.getLocation().getRequestIntervalMs())));
    }

    private Duration rateLimitWait(NhanhRateLimitException ex) {
        long bufferSeconds = Math.max(0, nhanhProperties.getLocation().getRateLimitUnlockBufferSeconds());
        if (ex.getUnlockedAt() != null) {
            return positive(Duration.between(clock.instant(), ex.getUnlockedAt().plusSeconds(bufferSeconds)));
        }
        if (ex.getLockedSeconds() != null) {
            return Duration.ofSeconds(Math.max(0, ex.getLockedSeconds() + bufferSeconds));
        }
        return Duration.ofSeconds(30 + bufferSeconds);
    }

    private Duration positive(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return Duration.ZERO;
        }
        return duration;
    }

    private void sleep(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Nhanh location rate-limit wait interrupted", ex);
        }
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
