package com.vn.sodu.order.mapper;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.order.dtos.OrderItemResponseDto;
import com.vn.sodu.order.dtos.OrderResponseDto;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.request.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderResponseMapper {

    private final ProductRepo productRepo;

    public OrderResponseDto toDto(Order order) {
        if (order == null) {
            return null;
        }
        Request request = order.getRequest();
        List<OrderItemResponseDto> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                    .map(this::toItemDto)
                    .toList();

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .requestId(request == null ? null : request.getId())
                .requestCode(request == null ? null : request.getRequestCode())
                .type(order.getType())
                .status(order.getStatus())
                .syncStatus(order.getSyncStatus())
                .nhanhSyncStage(order.getNhanhSyncStage())
                .totalAmount(order.getTotalAmount())
                .depositAmount(order.getDepositAmount())
                .shippingFee(order.getShippingFee())
                .paidAmount(order.getPaidAmount())
                .remainingAmount(order.getRemainingAmount())
                .paymentStatus(order.getPaymentStatus())
                .description(order.getDescription())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .customerEmail(order.getCustomerEmail())
                .customerAddress(order.getCustomerAddress())
                .customerCityName(order.getCustomerCityName())
                .customerDistrictName(order.getCustomerDistrictName())
                .customerWardName(order.getCustomerWardName())
                .customerCityId(order.getCustomerCityId())
                .customerDistrictId(order.getCustomerDistrictId())
                .customerWardId(order.getCustomerWardId())
                .nhanhOrderId(order.getNhanhOrderId())
                .nhanhOrderCode(order.getNhanhOrderCode())
                .syncError(order.getSyncError())
                .lastSyncMessage(order.getLastSyncMessage())
                .lastSyncAt(order.getLastSyncAt())
                .carrierId(order.getCarrierId())
                .carrierServiceId(order.getCarrierServiceId())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderItemResponseDto toItemDto(OrderItem item) {
        if (item == null) {
            return null;
        }
        Long productId = null;
        String nhanhId = item.getNhanhProductId();
        Long parsed = safeParseLong(nhanhId);
        if (parsed != null) {
            productId = productRepo.findByExternalId(parsed)
                    .map(Product::getId)
                    .orElse(null);
        }
        return OrderItemResponseDto.builder()
                .id(item.getId())
                .nhanhProductId(item.getNhanhProductId())
                .name(item.getName())
                .note(item.getNote())
                .price(item.getPrice())
                .discount(item.getDiscount())
                .quantity(item.getQuantity())
                .productId(productId)
                .build();
    }

    private static Long safeParseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
