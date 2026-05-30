package com.vn.sodu.order.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.services.OrderQueryService;
import com.vn.sodu.order.dtos.OrderSyncResultDto;
import com.vn.sodu.order.services.OrderSyncService;
import com.vn.sodu.order.dtos.OrderResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin/orders")
@Tag(name = "Admin Orders", description = "Admin endpoints for reading orders and retrying synchronization")
public class OrderSyncController {

    private final OrderSyncService orderSyncService;
    private final OrderQueryService orderQueryService;

    @GetMapping
    @Operation(
            summary = "List orders",
            description = "Returns converted internal orders for staff/admin users."
    )
    public ResponseEntity<ApiResponseDTO<PageResponse<OrderResponseDto>>> listOrders(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection
    ) {
        requireStaff(authentication);
        Page<OrderResponseDto> orders = orderQueryService.listOrders(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponseDTO.success(
                PageResponse.from(orders),
                "Orders retrieved",
                HttpStatus.OK.value()
        ));
    }

    @GetMapping("/{orderId}")
    @Operation(
            summary = "Get order detail",
            description = "Returns one converted internal order with sync state and items."
    )
    public ResponseEntity<ApiResponseDTO<OrderResponseDto>> getOrderDetail(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        requireStaff(authentication);
        OrderResponseDto order = orderQueryService.getOrderDetail(orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(
                order,
                "Order retrieved",
                HttpStatus.OK.value()
        ));
    }

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
