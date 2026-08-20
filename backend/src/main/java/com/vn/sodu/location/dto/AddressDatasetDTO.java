package com.vn.sodu.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDatasetDTO {
    private String version;
    private String source;
    private LocalDateTime importedAt;
    private String checksum;
    private long provinceCount;
    private long wardCount;
}