package com.vn.sodu.order.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDto {
    private String productId;
    private String nhanhProductId;
    private String name;
    private double price;
    private String imageUrl;
    private int quantity;
}
