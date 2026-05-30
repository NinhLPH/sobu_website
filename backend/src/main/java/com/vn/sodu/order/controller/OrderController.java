package com.vn.sodu.order.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.order.services.OrderQueryService;
import com.vn.sodu.order.dtos.OrderResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/orders", "/api/orders"})
@Tag(name = "Orders", description = "Authenticated customer order endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderQueryService orderQueryService;

    @GetMapping("/me/{orderId}")
    @Operation(
            summary = "Get my order detail",
            description = "Returns the authenticated customer's own order detail using the account phone binding."
    )
    public ResponseEntity<ApiResponseDTO<OrderResponseDto>> getMyOrderDetail(
            @Parameter(description = "Internal order identifier", required = true, example = "1")
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        OrderResponseDto response = orderQueryService.getMyOrderDetail(orderId, authentication);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Order retrieved", HttpStatus.OK.value()));
    }

    @GetMapping("/me/by-nhanh/{nhanhOrderId}")
    @Operation(
            summary = "Get my order by Nhanh order id",
            description = "Returns the authenticated customer's own order by Nhanh order id or Nhanh order code."
    )
    public ResponseEntity<ApiResponseDTO<OrderResponseDto>> getMyOrderByNhanhOrderId(
            @Parameter(description = "Nhanh order id or code", required = true, example = "123456")
            @PathVariable String nhanhOrderId,
            Authentication authentication
    ) {
        OrderResponseDto response = orderQueryService.getMyOrderDetailByNhanhOrderId(nhanhOrderId, authentication);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Order retrieved", HttpStatus.OK.value()));
    }
}
