package com.vn.sodu.ui.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaticPageRequest {
    @NotBlank
    @Size(max = 160)
    private String slug;

    @NotBlank
    @Size(max = 255)
    private String title;

    private String htmlContent;

    private Boolean isPublished;
}
