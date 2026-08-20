package com.vn.sodu.location;

import com.vn.sodu.location.dto.AddressDatasetDTO;
import com.vn.sodu.location.dto.ProvinceListResponseDTO;
import com.vn.sodu.location.dto.ProvinceResponseDTO;
import com.vn.sodu.location.dto.WardListResponseDTO;
import com.vn.sodu.location.dto.WardResponseDTO;
import com.vn.sodu.nhanh.location.LocationDataUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressControllerTest {

    private MockMvc mockMvcFor(AddressService addressService) {
        return MockMvcBuilders
                .standaloneSetup(new AddressController(addressService))
                .build();
    }

    @Test
    void getProvincesReturnsActiveProvincesWithDatasetVersion() throws Exception {
        AddressService addressService = mock(AddressService.class);
        when(addressService.listProvinces()).thenReturn(ProvinceListResponseDTO.builder()
                .datasetVersion("v2")
                .provinces(List.of(
                        new ProvinceResponseDTO(254L, "Ha Noi"),
                        new ProvinceResponseDTO(79L, "Ho Chi Minh")))
                .build());

        mockMvcFor(addressService).perform(get("/api/public/address/provinces")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Provinces retrieved"))
                .andExpect(jsonPath("$.data.datasetVersion").value("v2"))
                .andExpect(jsonPath("$.data.provinces[0].id").value(254))
                .andExpect(jsonPath("$.data.provinces[0].name").value("Ha Noi"))
                .andExpect(jsonPath("$.data.provinces[1].id").value(79));
    }

    @Test
    void getProvincesReturnsServiceUnavailableWhenDatasetEmpty() throws Exception {
        AddressService addressService = mock(AddressService.class);
        when(addressService.listProvinces()).thenThrow(new LocationDataUnavailableException());

        mockMvcFor(addressService).perform(get("/api/public/address/provinces"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("LOCATION_DATA_UNAVAILABLE"));
    }

    @Test
    void getWardsReturnsWardsForProvinceId() throws Exception {
        AddressService addressService = mock(AddressService.class);
        when(addressService.listWards(254L)).thenReturn(WardListResponseDTO.builder()
                .datasetVersion("v2")
                .wards(List.of(
                        new WardResponseDTO(1116L, "Phuc Xa"),
                        new WardResponseDTO(1117L, "Truc Bach")))
                .build());

        mockMvcFor(addressService).perform(get("/api/public/address/wards")
                        .param("provinceId", "254")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Wards retrieved"))
                .andExpect(jsonPath("$.data.datasetVersion").value("v2"))
                .andExpect(jsonPath("$.data.wards[0].id").value(1116))
                .andExpect(jsonPath("$.data.wards[0].name").value("Phuc Xa"));
    }

    @Test
    void getCurrentDatasetReturnsReleaseMetadata() throws Exception {
        AddressService addressService = mock(AddressService.class);
        when(addressService.currentDataset()).thenReturn(AddressDatasetDTO.builder()
                .version("2025.1")
                .source("VietnamProvinces")
                .importedAt(LocalDateTime.parse("2026-08-13T10:00:00"))
                .checksum("abc123")
                .provinceCount(34)
                .wardCount(3321)
                .build());

        mockMvcFor(addressService).perform(get("/api/public/address/datasets/current")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").value("2025.1"))
                .andExpect(jsonPath("$.data.source").value("VietnamProvinces"))
                .andExpect(jsonPath("$.data.provinceCount").value(34))
                .andExpect(jsonPath("$.data.wardCount").value(3321));
    }
}