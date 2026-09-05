package com.vn.sodu.order.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.integration.NhanhEnabled;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.dtos.OrderResponseDto;
import com.vn.sodu.order.dtos.OrderSyncResultDto;
import com.vn.sodu.order.dtos.UpdateOrderStatusDto;
import com.vn.sodu.order.mapper.OrderResponseMapper;
import com.vn.sodu.order.services.OrderQueryService;
import com.vn.sodu.order.services.OrderService;
import com.vn.sodu.order.services.OrderSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/admin/orders")
@Tag(name = "Admin Orders", description = "Admin endpoints for reading orders, updating status, and synchronization")
public class OrderSyncController {

    private final ObjectProvider<OrderSyncService> orderSyncServiceProvider;
    private final OrderQueryService orderQueryService;
    private final OrderService orderService;
    private final OrderResponseMapper orderResponseMapper;
    private final com.vn.sodu.order.services.OrderExportService orderExportService;
    private final NhanhEnabled nhanhEnabled;

    @PostMapping("/export/spx")
    @Operation(
            summary = "Export orders to SPX Excel",
            description = "Exports selected orders or filtered orders to SPX mass creation Excel format."
    )
    public ResponseEntity<byte[]> exportSpxExcelPost(
            Authentication authentication,
            @org.springframework.web.bind.annotation.RequestBody(required = false) com.vn.sodu.order.dtos.ExportOrdersRequestDto requestDto
    ) {
        requireStaff(authentication);
        List<Long> ids = requestDto != null ? requestDto.getIds() : null;
        String status = requestDto != null ? requestDto.getStatus() : null;
        String query = requestDto != null ? requestDto.getQuery() : null;

        byte[] excelBytes = orderExportService.exportSpxExcel(ids, status, query);
        String filename = "SPX_Orders_" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now()) + ".xlsx";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/export/spx")
    @Operation(
            summary = "Export orders to SPX Excel (GET)",
            description = "Exports selected orders or filtered orders to SPX mass creation Excel format via query params."
    )
    public ResponseEntity<byte[]> exportSpxExcelGet(
            Authentication authentication,
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query
    ) {
        requireStaff(authentication);
        byte[] excelBytes = orderExportService.exportSpxExcel(ids, status, query);
        String filename = "SPX_Orders_" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now()) + ".xlsx";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

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
        nhanhEnabled.requireEnabled();
        OrderSyncService orderSyncService = orderSyncServiceProvider.getIfAvailable();
        if (orderSyncService == null) {
            throw new IllegalStateException("Order sync service is unavailable");
        }
        Order order = orderSyncService.retryOrderSync(orderId);
        return ResponseEntity.ok(ApiResponseDTO.success(
                OrderSyncResultDto.from(order),
                "Order sync retry completed",
                HttpStatus.OK.value()
        ));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(
            summary = "Update order status",
            description = "Allows staff/admin to manually advance or update an order status according to the Stage Transition Matrix."
    )
    public ResponseEntity<ApiResponseDTO<OrderResponseDto>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusDto dto,
            Authentication authentication
    ) {
        return handleStatusUpdate(orderId, dto, authentication);
    }

    @PutMapping("/{orderId}/status")
    @Operation(
            summary = "Update order status (PUT)",
            description = "Allows staff/admin to manually advance or update an order status according to the Stage Transition Matrix."
    )
    public ResponseEntity<ApiResponseDTO<OrderResponseDto>> updateOrderStatusPut(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusDto dto,
            Authentication authentication
    ) {
        return handleStatusUpdate(orderId, dto, authentication);
    }

    private ResponseEntity<ApiResponseDTO<OrderResponseDto>> handleStatusUpdate(
            Long orderId,
            UpdateOrderStatusDto dto,
            Authentication authentication
    ) {
        requireStaff(authentication);
        Order order = orderService.updateOrderStatusAsAdmin(orderId, dto, authentication);
        OrderResponseDto response = orderResponseMapper.toDto(order);
        return ResponseEntity.ok(ApiResponseDTO.success(
                response,
                "Order status updated successfully",
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
