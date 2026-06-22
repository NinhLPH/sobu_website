package com.vn.sodu.nhanh.dto;

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
public class LocationWardDTO {
    private Long wardId;
    private String wardName;
    private String otherName;
}
