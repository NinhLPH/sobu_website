package com.vn.sodu.global.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SearchRequest {
    @NotBlank(message = "Search term is required")
    @Size(min = 1, max = 255, message = "Search term must be between 1 and 255 characters")
    private String searchTerm;

    @PositiveOrZero(message = "Page number must be greater than or equal to 0")
    private int page = 0;

    @Positive(message = "Page size must be greater than 0")
    @Max(value = 100, message = "Page size must not exceed 100")
    private int pageSize = 20;

    @NotBlank(message = "Sort field cannot be blank")
    private String sortBy = "id";

    @Pattern(regexp = "ASC|DESC|asc|desc", message = "Sort direction must be ASC or DESC")
    private String sortDirection = "DESC";
}
