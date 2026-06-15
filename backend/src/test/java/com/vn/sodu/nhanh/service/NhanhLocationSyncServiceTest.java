package com.vn.sodu.nhanh.service;

import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.dto.NhanhLocationItemDTO;
import com.vn.sodu.nhanh.location.NhanhLocationExtendedLockException;
import com.vn.sodu.nhanh.location.NhanhLocationRateLimiter;
import com.vn.sodu.nhanh.location.NhanhLocationSleeper;
import com.vn.sodu.nhanh.location.NhanhLocationSnapshotStore;
import com.vn.sodu.product.dto.NhanhResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NhanhLocationSyncServiceTest {

    private NhanhClient client;
    private NhanhService nhanhService;
    private NhanhLocationSnapshotStore store;
    private NhanhLocationRateLimiter limiter;
    private NhanhLocationSleeper sleeper;
    private NhanhProperties properties;
    private Clock clock;

    @BeforeEach
    void setUp() {
        client = mock(NhanhClient.class);
        nhanhService = mock(NhanhService.class);
        store = mock(NhanhLocationSnapshotStore.class);
        limiter = mock(NhanhLocationRateLimiter.class);
        sleeper = mock(NhanhLocationSleeper.class);
        properties = new NhanhProperties();
        properties.setClientId("app");
        properties.setClientSecret("secret");
        properties.setRedirectUri("http://callback");
        properties.setBusinessId("123");
        properties.getLocation().setInterChunkSleepMs(0);
        clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        doNothing().when(limiter).acquire();
    }

    @Test
    void discoveryFetchesOneCityListAndOneDistrictListPerCity() {
        when(client.postOnce(anyString(), eq("token"), any(), any()))
                .thenAnswer(invocation -> {
                    Map<?, ?> filters = filters(invocation.getArgument(2));
                    return switch ((String) filters.get("type")) {
                        case "CITY" -> response(List.of(item(1), item(2)));
                        case "DISTRICT" -> response(List.of(item(
                                100 + ((Number) filters.get("parentId")).longValue())));
                        case "WARD" -> response(List.of(item(
                                1000 + ((Number) filters.get("parentId")).longValue())));
                        default -> response(List.of());
                    };
                });

        service().synchronize();

        verify(client, times(5)).postOnce(anyString(), eq("token"), any(), any());
        verify(store).save(any(LocationTreeResponse.class), eq(2), eq(2), eq(2));
    }

    @Test
    void balancesCitiesWithoutExceedingChunkCapacity() {
        properties.getLocation().setChunkSize(2);
        List<NhanhLocationSyncService.CityDiscovery> cities = new ArrayList<>();
        cities.add(discovery(1, 5));
        cities.add(discovery(2, 4));
        cities.add(discovery(3, 3));
        cities.add(discovery(4, 2));
        cities.add(discovery(5, 1));

        List<List<NhanhLocationSyncService.CityDiscovery>> chunks =
                service().buildBalancedChunks(cities);

        assertEquals(3, chunks.size());
        assertEquals(5, chunks.stream().mapToInt(List::size).sum());
        assertEquals(2, chunks.stream().mapToInt(List::size).max().orElseThrow());
    }

    @Test
    void extendedRateLimitAbortsWithoutSavingSnapshot() {
        properties.getLocation().setExtendedLockThresholdMs(300_000);
        when(client.postOnce(anyString(), eq("token"), any(), any()))
                .thenThrow(new NhanhApiException(
                        "locked",
                        429,
                        "ERR_429",
                        Instant.parse("2026-06-15T00:10:00Z"),
                        false,
                        null));

        assertThrows(NhanhLocationExtendedLockException.class, () -> service().synchronize());
        verify(store, times(0)).save(any(), anyInt(), anyInt(), anyInt());
    }

    private NhanhLocationSyncService service() {
        return new NhanhLocationSyncService(
                client,
                nhanhService,
                properties,
                new NhanhLocationMapper(),
                store,
                limiter,
                sleeper,
                clock);
    }

    private NhanhLocationSyncService.CityDiscovery discovery(long id, int districtCount) {
        List<NhanhLocationItemDTO> districts = new ArrayList<>();
        for (int i = 0; i < districtCount; i++) {
            districts.add(item(id * 100 + i));
        }
        return new NhanhLocationSyncService.CityDiscovery(item(id), districts);
    }

    private Map<?, ?> filters(Object body) {
        return (Map<?, ?>) ((Map<?, ?>) body).get("filters");
    }

    private NhanhResponse<List<NhanhLocationItemDTO>> response(
            List<NhanhLocationItemDTO> items) {
        return new NhanhResponse<>(1, items, null);
    }

    private NhanhLocationItemDTO item(long id) {
        NhanhLocationItemDTO item = new NhanhLocationItemDTO();
        item.setId(id);
        item.setName("Location " + id);
        return item;
    }
}
