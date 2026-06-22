package com.vn.sodu.nhanh.controller;

import com.vn.sodu.nhanh.dto.LocationCityDTO;
import com.vn.sodu.nhanh.dto.LocationDistrictDTO;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.dto.LocationWardDTO;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.location.LocationDataUnavailableException;
import com.vn.sodu.nhanh.service.NhanhLocationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NhanhLocationControllerTest {

    @Test
    void getLocationsReturnsPublicLocationTreeWithoutAuthentication() throws Exception {
        NhanhLocationService locationService = mock(NhanhLocationService.class);
        NhanhProperties properties = new NhanhProperties();
        when(locationService.getLocations()).thenReturn(LocationTreeResponse.builder()
                .provider("NHANH")
                .locationVersion("v1")
                .cachedAt(Instant.parse("2026-06-13T03:00:00Z"))
                .expiresAt(Instant.parse("2026-06-14T03:00:00Z"))
                .stale(false)
                .cities(List.of(LocationCityDTO.builder()
                        .cityId(254L)
                        .cityName("Ha Noi")
                        .districts(List.of(LocationDistrictDTO.builder()
                                .districtId(331L)
                                .districtName("Ba Dinh")
                                .wards(List.of(LocationWardDTO.builder()
                                        .wardId(1116L)
                                        .wardName("Phuc Xa")
                                        .build()))
                                .build()))
                        .build()))
                .build());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new NhanhLocationController(locationService, properties))
                .build();

        mockMvc.perform(get("/api/public/locations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Locations retrieved successfully"))
                .andExpect(jsonPath("$.data.provider").value("NHANH"))
                .andExpect(jsonPath("$.data.locationVersion").value("v1"))
                .andExpect(jsonPath("$.data.stale").value(false))
                .andExpect(jsonPath("$.data.cities[0].cityId").value(254))
                .andExpect(jsonPath("$.data.cities[0].districts[0].districtId").value(331))
                .andExpect(jsonPath("$.data.cities[0].districts[0].wards[0].wardId").value(1116));
    }

    @Test
    void getLocationsUsesStaleCacheMessage() throws Exception {
        NhanhLocationService locationService = mock(NhanhLocationService.class);
        NhanhProperties properties = new NhanhProperties();
        when(locationService.getLocations()).thenReturn(LocationTreeResponse.builder()
                .provider("NHANH")
                .locationVersion("v1")
                .cachedAt(Instant.parse("2026-06-13T03:00:00Z"))
                .expiresAt(Instant.parse("2026-06-14T03:00:00Z"))
                .stale(true)
                .cities(List.of())
                .build());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new NhanhLocationController(locationService, properties))
                .build();

        mockMvc.perform(get("/api/public/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Locations retrieved from stale cache"))
                .andExpect(jsonPath("$.data.stale").value(true));
    }

    @Test
    void getLocationsReturnsRetryAfterWhenSnapshotIsMissing() throws Exception {
        NhanhLocationService locationService = mock(NhanhLocationService.class);
        NhanhProperties properties = new NhanhProperties();
        properties.getLocation().setRetryAfterSeconds(30);
        when(locationService.getLocations()).thenThrow(new LocationDataUnavailableException());
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new NhanhLocationController(locationService, properties))
                .build();

        mockMvc.perform(get("/api/public/locations"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.error").value("LOCATION_DATA_UNAVAILABLE"));
    }
}
