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
public class LocationDistrictDTO {
    private Long districtId;
    private String districtName;
    private String otherName;
    private List<LocationWardDTO> wards;
}
