package com.vn.sodu.nhanh.service;

import com.vn.sodu.nhanh.dto.LocationCityDTO;
import com.vn.sodu.nhanh.dto.LocationDistrictDTO;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.dto.LocationWardDTO;
import com.vn.sodu.nhanh.dto.NhanhLocationItemDTO;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class NhanhLocationMapper {

    public LocationWardDTO toWard(NhanhLocationItemDTO item) {
        return LocationWardDTO.builder()
                .wardId(item.getId())
                .wardName(item.getName())
                .otherName(item.getOtherName())
                .build();
    }

    public LocationDistrictDTO toDistrict(NhanhLocationItemDTO item, List<LocationWardDTO> wards) {
        return LocationDistrictDTO.builder()
                .districtId(item.getId())
                .districtName(item.getName())
                .otherName(item.getOtherName())
                .wards(wards)
                .build();
    }

    public LocationCityDTO toCity(NhanhLocationItemDTO item, List<LocationDistrictDTO> districts) {
        return LocationCityDTO.builder()
                .cityId(item.getId())
                .cityName(item.getName())
                .otherName(item.getOtherName())
                .districts(districts)
                .build();
    }

    public LocationTreeResponse toTree(
            String locationVersion,
            Instant cachedAt,
            Instant expiresAt,
            boolean stale,
            List<LocationCityDTO> cities) {
        return LocationTreeResponse.builder()
                .provider("NHANH")
                .locationVersion(locationVersion)
                .cachedAt(cachedAt)
                .expiresAt(expiresAt)
                .stale(stale)
                .cities(cities)
                .build();
    }

    public LocationTreeResponse withStale(LocationTreeResponse data, boolean stale) {
        return data.toBuilder()
                .stale(stale)
                .build();
    }
}
