package com.vn.sodu.order.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNormalOrderItemDto {

    @NotBlank(message = "Nhanh product id is required")
    @Size(max = 50, message = "Nhanh product id must not exceed 50 characters")
    private String nhanhProductId;

    @NotBlank(message = "Item name is required")
    @Size(max = 255, message = "Item name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Item note must not exceed 1000 characters")
    private String note;

    @NotNull(message = "Item price is required")
    @DecimalMin(value = "0.00", message = "Item price must be greater than or equal to 0")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "Item discount must be greater than or equal to 0")
    private BigDecimal discount;

    @NotNull(message = "Item quantity is required")
    @Min(value = 1, message = "Item quantity must be at least 1")
    private Integer quantity;
}
