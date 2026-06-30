package com.vn.sodu.order.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDto {
    @Builder.Default
    private List<CartItemDto> items = new ArrayList<>();
    private Instant updatedAt;
}
