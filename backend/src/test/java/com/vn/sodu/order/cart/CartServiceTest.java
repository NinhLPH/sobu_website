package com.vn.sodu.order.cart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private RedisTemplate<String, CartDto> cartRedisTemplate;

    @Mock
    private ValueOperations<String, CartDto> valueOperations;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        when(cartRedisTemplate.opsForValue()).thenReturn(valueOperations);
        cartService = new CartService(cartRedisTemplate);
    }

    @Test
    void getCartReturnsEmptyCartWhenRedisHasNoValue() {
        when(valueOperations.get("cart:user@example.com")).thenReturn(null);

        CartDto cart = cartService.getCart("user@example.com");

        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getUpdatedAt()).isNull();
    }

    @Test
    void addItemSavesCartWithTtlAndNormalizesNullItemList() {
        CartDto existingCart = CartDto.builder()
                .items(null)
                .build();
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("10")
                .nhanhProductId("90010")
                .name("Serum phuc hoi")
                .price(350000.0)
                .imageUrl("/images/products/10.jpg")
                .quantity(2)
                .build();
        when(valueOperations.get("cart:user@example.com")).thenReturn(existingCart);
        ArgumentCaptor<CartDto> cartCaptor = ArgumentCaptor.forClass(CartDto.class);

        cartService.addItem("user@example.com", request);

        verify(valueOperations).set(
                eq("cart:user@example.com"),
                cartCaptor.capture(),
                eq(30L),
                eq(TimeUnit.MINUTES)
        );
        CartDto savedCart = cartCaptor.getValue();
        assertThat(savedCart.getUpdatedAt()).isNotNull();
        assertThat(savedCart.getItems()).hasSize(1);
        assertThat(savedCart.getItems().get(0).getProductId()).isEqualTo("10");
        assertThat(savedCart.getItems().get(0).getNhanhProductId()).isEqualTo("90010");
        assertThat(savedCart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addItemReplacesExistingProductInsteadOfDuplicatingIt() {
        CartItemDto existingItem = CartItemDto.builder()
                .productId("10")
                .name("Old name")
                .price(100000.0)
                .quantity(1)
                .build();
        CartDto existingCart = CartDto.builder()
                .items(List.of(existingItem))
                .build();
        AddToCartRequest request = AddToCartRequest.builder()
                .productId("10")
                .name("New name")
                .price(200000.0)
                .quantity(3)
                .build();
        when(valueOperations.get("cart:user@example.com")).thenReturn(existingCart);
        ArgumentCaptor<CartDto> cartCaptor = ArgumentCaptor.forClass(CartDto.class);

        cartService.addItem("user@example.com", request);

        verify(valueOperations).set(
                eq("cart:user@example.com"),
                cartCaptor.capture(),
                eq(30L),
                eq(TimeUnit.MINUTES)
        );
        assertThat(cartCaptor.getValue().getItems()).hasSize(1);
        assertThat(cartCaptor.getValue().getItems().get(0).getName()).isEqualTo("New name");
        assertThat(cartCaptor.getValue().getItems().get(0).getQuantity()).isEqualTo(3);
    }
}
