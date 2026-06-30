package com.vn.sodu.order.cart;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.user.Account;
import com.vn.sodu.user.AccountRepo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/cart", "/api/cart"})
@Tag(name = "Cart", description = "Authenticated customer cart endpoints backed by Redis")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;
    private final AccountRepo accountRepo;

    @GetMapping
    @Operation(summary = "Get current cart", description = "Returns the authenticated user's cart from Redis")
    public ResponseEntity<ApiResponseDTO<CartDto>> getCart(Authentication authentication) {
        String email = resolveCustomerEmail(authentication);
        CartDto cart = cartService.getCart(email);
        return ResponseEntity.ok(ApiResponseDTO.success(cart, "Cart retrieved"));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds or updates an item in the cart")
    public ResponseEntity<ApiResponseDTO<CartDto>> addItem(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication
    ) {
        String email = resolveCustomerEmail(authentication);
        cartService.addItem(email, request);
        CartDto cart = cartService.getCart(email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(cart, "Item added to cart"));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of an item in the cart")
    public ResponseEntity<ApiResponseDTO<CartDto>> updateItemQuantity(
            @PathVariable String productId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication
    ) {
        String email = resolveCustomerEmail(authentication);
        cartService.updateItemQuantity(email, productId, request.getQuantity());
        CartDto cart = cartService.getCart(email);
        return ResponseEntity.ok(ApiResponseDTO.success(cart, "Cart item updated"));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from cart", description = "Removes an item from the cart")
    public ResponseEntity<ApiResponseDTO<Void>> removeItem(
            @PathVariable String productId,
            Authentication authentication
    ) {
        String email = resolveCustomerEmail(authentication);
        cartService.removeItem(email, productId);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Item removed from cart"));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Clears all items from the cart")
    public ResponseEntity<ApiResponseDTO<Void>> clearCart(Authentication authentication) {
        String email = resolveCustomerEmail(authentication);
        cartService.clearCart(email);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Cart cleared"));
    }

    private String resolveCustomerEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }

        Account account = accountRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated account not found"));

        if (account.getEmail() == null || account.getEmail().isBlank()) {
            throw new AccessDeniedException("Authenticated account does not have an email address");
        }
        return account.getEmail().trim();
    }
}
