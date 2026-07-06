package com.vn.sodu.nhanh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ward/commune information")
public class LocationWardDTO {
    @Schema(description = "Ward ID", example = "1")
    private Long wardId;

    @Schema(description = "Ward name", example = "Phường Bến Nghé")
    private String wardName;

    @Schema(description = "Alternative name")
    private String otherName;
}
