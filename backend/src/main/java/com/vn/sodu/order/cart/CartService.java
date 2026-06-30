package com.vn.sodu.order.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_TTL_MINUTES = 30;

    private final RedisTemplate<String, CartDto> cartRedisTemplate;

    public CartDto getCart(String email) {
        String key = buildKey(email);
        CartDto cart = cartRedisTemplate.opsForValue().get(key);
        return normalizeCart(cart);
    }

    public void addItem(String email, AddToCartRequest request) {
        String key = buildKey(email);
        CartDto cart = getCart(email);

        CartItemDto newItem = CartItemDto.builder()
                .productId(request.getProductId())
                .nhanhProductId(request.getNhanhProductId())
                .name(request.getName())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .quantity(request.getQuantity())
                .build();

        cart.getItems().removeIf(item -> item.getProductId().equals(request.getProductId()));
        cart.getItems().add(newItem);
        cart.setUpdatedAt(Instant.now());

        saveCart(key, cart);
    }

    public void updateItemQuantity(String email, String productId, int quantity) {
        String key = buildKey(email);
        CartDto cart = getCart(email);

        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(quantity);
                    cart.setUpdatedAt(Instant.now());
                    saveCart(key, cart);
                });
    }

    public void removeItem(String email, String productId) {
        String key = buildKey(email);
        CartDto cart = getCart(email);

        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (removed) {
            cart.setUpdatedAt(Instant.now());
            saveCart(key, cart);
        }
    }

    public void clearCart(String email) {
        String key = buildKey(email);
        cartRedisTemplate.delete(key);
    }

    private void saveCart(String key, CartDto cart) {
        cartRedisTemplate.opsForValue().set(key, cart, CART_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private CartDto normalizeCart(CartDto cart) {
        CartDto safeCart = cart != null ? cart : new CartDto();
        if (safeCart.getItems() == null) {
            safeCart.setItems(new ArrayList<>());
        } else {
            safeCart.setItems(new ArrayList<>(safeCart.getItems()));
        }
        return safeCart;
    }

    private String buildKey(String email) {
        return CART_KEY_PREFIX + email;
    }
}
