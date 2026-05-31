package com.vn.sodu.order.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNormalOrderDto {

    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    private String customerName;

    @NotBlank(message = "Customer mobile is required")
    @Size(max = 20, message = "Customer mobile must not exceed 20 characters")
    private String customerMobile;

    @Size(max = 255, message = "Customer email must not exceed 255 characters")
    private String customerEmail;

    @Size(max = 500, message = "Customer address must not exceed 500 characters")
    private String customerAddress;

    @Size(max = 100, message = "Customer city must not exceed 100 characters")
    private String customerCityName;

    @Size(max = 100, message = "Customer district must not exceed 100 characters")
    private String customerDistrictName;

    @Size(max = 100, message = "Customer ward must not exceed 100 characters")
    private String customerWardName;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotEmpty(message = "At least one order item is required")
    @Valid
    private List<CreateNormalOrderItemDto> items;
}
