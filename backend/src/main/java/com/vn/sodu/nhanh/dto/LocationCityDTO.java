package com.vn.sodu.nhanh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "City/province information")
public class LocationCityDTO {
    @Schema(description = "City/province ID", example = "1")
    private Long cityId;

    @Schema(description = "City/province name", example = "Hồ Chí Minh")
    private String cityName;

    @Schema(description = "Alternative name", example = "Sài Gòn")
    private String otherName;

    @Schema(description = "List of districts in this city")
    private List<LocationDistrictDTO> districts;
}
