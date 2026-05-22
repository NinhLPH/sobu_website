package com.vn.sodu.request.dto;

import com.vn.sodu.request.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreateRequestDto {

    @NotBlank(message = "Customer phone is required")
    @Size(max = 20, message = "Customer phone must not exceed 20 characters")
    private String customerPhone;

    @NotNull(message = "Request type is required")
    private OrderType type;

    @NotEmpty(message = "At least one request item is required")
    @Valid
    private List<RequestItemDto> items;

    @Size(max = 5000, message = "Custom requirements must not exceed 5000 characters")
    private String customRequirements;

    @Size(max = 50, message = "Uploaded images must not exceed 50 entries")
    private List<@Size(max = 500, message = "Image URL must not exceed 500 characters") String> uploadedImageUrls;
}
