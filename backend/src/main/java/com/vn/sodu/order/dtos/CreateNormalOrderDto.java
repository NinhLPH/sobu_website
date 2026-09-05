package com.vn.sodu.order.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @Deprecated
    @Size(max = 255, message = "Customer street must not exceed 255 characters")
    private String customerStreet;

    @Deprecated
    @Size(max = 255, message = "Customer hamlet must not exceed 255 characters")
    private String customerHamlet;

    @Size(max = 100, message = "Customer city must not exceed 100 characters")
    private String customerCityName;

    @Size(max = 100, message = "Customer district must not exceed 100 characters")
    private String customerDistrictName;

    @Size(max = 100, message = "Customer ward must not exceed 100 characters")
    private String customerWardName;

    private Long customerCityId;

    private Long customerDistrictId;

    private Long customerWardId;

    @Size(max = 20, message = "Location contract version must not exceed 20 characters")
    private String locationVersion;

    private Long carrierId;

    private Long carrierServiceId;

    private java.math.BigDecimal shippingFee;

    private String discountVoucherCode;

    private String shippingVoucherCode;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotEmpty(message = "At least one order item is required")
    @Valid
    private List<CreateNormalOrderItemDto> items;
}
