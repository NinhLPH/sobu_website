package com.vn.sodu.nhanh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationCityDTO {
    private Long cityId;
    private String cityName;
    private String otherName;
    private List<LocationDistrictDTO> districts;
}
