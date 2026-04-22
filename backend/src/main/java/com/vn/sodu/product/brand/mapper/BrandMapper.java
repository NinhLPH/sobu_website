package com.vn.sodu.product.brand.mapper;

import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class BrandMapper {

    public Brand toEntity(NhanhBrandDTO dto) {
        if (dto == null) {
            return null;
        }

        return Brand.builder()
                .id(dto.getId())
                .parentId(dto.getParentId())
                .code(dto.getCode())
                .name(dto.getName())
                .status(dto.getStatus())
                .createdAt(toLocalDateTime(dto.getCreatedAt()))
                .build();
    }

    private LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }

        long normalized = timestamp;
        if (Math.abs(normalized) < 1_000_000_000_000L) {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(normalized), ZoneId.systemDefault());
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(normalized), ZoneId.systemDefault());
    }
}
