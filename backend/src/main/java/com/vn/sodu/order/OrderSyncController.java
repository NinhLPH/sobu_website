package com.vn.sodu.order;

import com.vn.sodu.global.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin/orders")
@Tag(name = "Admin Order Sync", description = "Admin endpoints for retrying order synchronization")
public class OrderSyncController {

    private final OrderSyncService orderSyncService;

    @PostMapping("/{orderId}/sync/retry")
    @Operation(
            summary = "Retry order sync",
            description = "Retries synchronization of an internal order to Nhanh."
    )
    public ResponseEntity<ApiResponseDTO<OrderSyncResultDto>> retryOrderSync(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        requireStaff(authentication);
        Order order = orderSyncService.retryOrderSync(orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(
                OrderSyncResultDto.from(order),
                "Order sync retry completed",
                HttpStatus.OK.value()
        ));
    }

    private void requireStaff(Authentication authentication) {
        if (!isStaff(authentication)) {
            throw new AccessDeniedException("Staff access is required");
        }
    }

    private boolean isStaff(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }
            String name = authority.getAuthority();
            if (name.equals("ROLE_ADMIN") || name.equals("ROLE_STAFF")) {
                return true;
            }
        }
        return false;
    }
}
