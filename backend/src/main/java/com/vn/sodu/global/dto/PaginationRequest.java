package com.vn.sodu.global.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginationRequest {

    @PositiveOrZero
    private int page = 0;

    @Positive
    @Max(100)
    private int pageSize = 10;

    private String sortBy = "id";

    private Sort.Direction sortDirection = Sort.Direction.ASC;

    public Pageable toPageable() {
        return PageRequest.of(
                page,
                pageSize,
                Sort.by(sortDirection, sortBy)
        );
    }
}
