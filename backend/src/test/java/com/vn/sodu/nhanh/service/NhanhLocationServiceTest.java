package com.vn.sodu.nhanh.service;

import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.dto.NhanhLocationItemDTO;
import com.vn.sodu.product.dto.NhanhResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NhanhLocationServiceTest {

    private NhanhClient nhanhClient;
    private NhanhService nhanhService;
    private NhanhProperties nhanhProperties;
    private MutableClock clock;
    private NhanhLocationService locationService;

    @BeforeEach
    void setUp() {
        nhanhClient = mock(NhanhClient.class);
        nhanhService = mock(NhanhService.class);
        nhanhProperties = new NhanhProperties();
        nhanhProperties.setClientId("77323");
        nhanhProperties.setClientSecret("secret");
        nhanhProperties.setRedirectUri("http://localhost/callback");
        nhanhProperties.setBusinessId("224003");
        nhanhProperties.getLocation().setPath("/v3.0/shipping/location");
        nhanhProperties.getLocation().setVersion("v1");
        nhanhProperties.getLocation().setCacheTtlHours(24);
        clock = new MutableClock(Instant.parse("2026-06-13T03:00:00Z"));
        locationService = new NhanhLocationService(
                nhanhClient,
                nhanhService,
                nhanhProperties,
                new NhanhLocationMapper(),
                clock
        );
    }

    @Test
    void coldCacheFetchesTreeAndSecondCallUsesCache() {
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        stubSimpleLocationTree();

        LocationTreeResponse first = locationService.getLocations();
        LocationTreeResponse second = locationService.getLocations();

        assertFalse(first.isStale());
        assertEquals("NHANH", first.getProvider());
        assertEquals("v1", first.getLocationVersion());
        assertEquals(Instant.parse("2026-06-13T03:00:00Z"), first.getCachedAt());
        assertEquals(Instant.parse("2026-06-14T03:00:00Z"), first.getExpiresAt());
        assertEquals(254L, first.getCities().get(0).getCityId());
        assertEquals(331L, first.getCities().get(0).getDistricts().get(0).getDistrictId());
        assertEquals(1116L, first.getCities().get(0).getDistricts().get(0).getWards().get(0).getWardId());
        assertSame(first, second);
        verify(nhanhService, times(1)).getValidAccessToken();
        verify(nhanhClient, times(3)).postWithBearerAuthorization(anyString(), eq("token"), any(), any());
    }

    @Test
    void expiresAtEqualNowIsExpiredAndRefreshes() {
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        stubSimpleLocationTree();

        locationService.getLocations();
        clock.advance(Duration.ofHours(24));
        LocationTreeResponse refreshed = locationService.getLocations();

        assertEquals(Instant.parse("2026-06-14T03:00:00Z"), refreshed.getCachedAt());
        verify(nhanhService, times(2)).getValidAccessToken();
        verify(nhanhClient, times(6)).postWithBearerAuthorization(anyString(), eq("token"), any(), any());
    }

    @Test
    void scheduledRefreshUpdatesCacheEvenWhenCurrentCacheIsStillValid() {
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        stubSimpleLocationTree();

        locationService.getLocations();
        clock.advance(Duration.ofHours(1));
        locationService.refreshLocationsCache();
        LocationTreeResponse refreshed = locationService.getLocations();

        assertEquals(Instant.parse("2026-06-13T04:00:00Z"), refreshed.getCachedAt());
        assertEquals(Instant.parse("2026-06-14T04:00:00Z"), refreshed.getExpiresAt());
        verify(nhanhService, times(2)).getValidAccessToken();
        verify(nhanhClient, times(6)).postWithBearerAuthorization(anyString(), eq("token"), any(), any());
    }

    @Test
    void expiredCacheRefreshFailureReturnsStaleCache() {
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        stubSimpleLocationTree();
        LocationTreeResponse fresh = locationService.getLocations();

        reset(nhanhClient, nhanhService);
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhClient.postWithBearerAuthorization(anyString(), eq("token"), any(), any()))
                .thenThrow(new ExternalServiceException("Nhanh unavailable"));
        clock.advance(Duration.ofHours(25));

        LocationTreeResponse stale = locationService.getLocations();

        assertTrue(stale.isStale());
        assertEquals(fresh.getCities().get(0).getCityId(), stale.getCities().get(0).getCityId());
        verify(nhanhService, times(1)).getValidAccessToken();
        verify(nhanhClient, times(1)).postWithBearerAuthorization(anyString(), eq("token"), any(), any());
    }

    @Test
    void noCacheRefreshFailureThrowsException() {
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhClient.postWithBearerAuthorization(anyString(), eq("token"), any(), any()))
                .thenThrow(new ExternalServiceException("Nhanh unavailable"));

        assertThrows(ExternalServiceException.class, () -> locationService.getLocations());
    }

    @Test
    void requestCountsMatchCityDistrictAndWardTreeShape() {
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        AtomicInteger cityCalls = new AtomicInteger();
        AtomicInteger districtCalls = new AtomicInteger();
        AtomicInteger wardCalls = new AtomicInteger();
        List<NhanhLocationItemDTO> cities = IntStream.rangeClosed(1, 63)
                .mapToObj(i -> item((long) i, "City " + i))
                .toList();

        when(nhanhClient.postWithBearerAuthorization(anyString(), eq("token"), any(), any()))
                .thenAnswer(invocation -> {
                    Map<?, ?> filters = filters(invocation.getArgument(2));
                    String type = (String) filters.get("type");
                    if ("CITY".equals(type)) {
                        cityCalls.incrementAndGet();
                        return response(cities);
                    }
                    if ("DISTRICT".equals(type)) {
                        districtCalls.incrementAndGet();
                        Long cityId = ((Number) filters.get("parentId")).longValue();
                        return response(List.of(item(1000L + cityId, "District " + cityId)));
                    }
                    wardCalls.incrementAndGet();
                    Long districtId = ((Number) filters.get("parentId")).longValue();
                    return response(List.of(item(2000L + districtId, "Ward " + districtId)));
                });

        LocationTreeResponse result = locationService.getLocations();

        assertEquals(63, result.getCities().size());
        assertEquals(1, cityCalls.get());
        assertEquals(63, districtCalls.get());
        assertEquals(63, wardCalls.get());
    }

    @Test
    void concurrentColdCacheRequestsRefreshOnlyOnce() throws Exception {
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        stubTwoCityTree();
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<LocationTreeResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return locationService.getLocations();
            }));
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        LocationTreeResponse first = futures.get(0).get(5, TimeUnit.SECONDS);
        for (Future<LocationTreeResponse> future : futures) {
            assertSame(first, future.get(5, TimeUnit.SECONDS));
        }

        executor.shutdownNow();
        verify(nhanhService, times(1)).getValidAccessToken();
        verify(nhanhClient, times(5)).postWithBearerAuthorization(anyString(), eq("token"), any(), any());
    }

    private void stubSimpleLocationTree() {
        when(nhanhClient.postWithBearerAuthorization(anyString(), eq("token"), any(), any()))
                .thenAnswer(invocation -> {
                    Map<?, ?> filters = filters(invocation.getArgument(2));
                    return switch ((String) filters.get("type")) {
                        case "CITY" -> response(List.of(item(254L, "Ha Noi")));
                        case "DISTRICT" -> response(List.of(item(331L, "Ba Dinh")));
                        case "WARD" -> response(List.of(item(1116L, "Phuc Xa")));
                        default -> response(List.of());
                    };
                });
    }

    private void stubTwoCityTree() {
        when(nhanhClient.postWithBearerAuthorization(anyString(), eq("token"), any(), any()))
                .thenAnswer(invocation -> {
                    Map<?, ?> filters = filters(invocation.getArgument(2));
                    String type = (String) filters.get("type");
                    if ("CITY".equals(type)) {
                        return response(List.of(item(1L, "City 1"), item(2L, "City 2")));
                    }
                    if ("DISTRICT".equals(type)) {
                        Long parentId = ((Number) filters.get("parentId")).longValue();
                        return response(List.of(item(parentId * 10, "District " + parentId)));
                    }
                    Long parentId = ((Number) filters.get("parentId")).longValue();
                    return response(List.of(item(parentId * 10, "Ward " + parentId)));
                });
    }

    private Map<?, ?> filters(Object body) {
        return (Map<?, ?>) ((Map<?, ?>) body).get("filters");
    }

    private NhanhResponse<List<NhanhLocationItemDTO>> response(List<NhanhLocationItemDTO> items) {
        return new NhanhResponse<>(1, items, null);
    }

    private NhanhLocationItemDTO item(Long id, String name) {
        NhanhLocationItemDTO item = new NhanhLocationItemDTO();
        item.setId(id);
        item.setName(name);
        return item;
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
