package com.vn.sodu.order.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {
    @NotBlank
    private String productId;

    private String nhanhProductId;

    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    private Double price;

    private String imageUrl;

    @Min(1)
    private int quantity;
}
