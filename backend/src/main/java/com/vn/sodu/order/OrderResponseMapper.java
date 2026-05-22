package com.vn.sodu.order;

import com.vn.sodu.request.Request;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderResponseMapper {

    public OrderResponseDto toDto(Order order) {
        if (order == null) {
            return null;
        }
        Request request = order.getRequest();
        List<OrderItemResponseDto> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                    .map(OrderItemResponseDto::from)
                    .toList();

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .requestId(request == null ? null : request.getId())
                .requestCode(request == null ? null : request.getRequestCode())
                .type(order.getType())
                .status(order.getStatus())
                .syncStatus(order.getSyncStatus())
                .totalAmount(order.getTotalAmount())
                .depositAmount(order.getDepositAmount())
                .description(order.getDescription())
                .customerName(order.getCustomerName())
                .customerMobile(order.getCustomerMobile())
                .customerEmail(order.getCustomerEmail())
                .customerAddress(order.getCustomerAddress())
                .customerCityName(order.getCustomerCityName())
                .customerDistrictName(order.getCustomerDistrictName())
                .customerWardName(order.getCustomerWardName())
                .nhanhOrderId(order.getNhanhOrderId())
                .nhanhOrderCode(order.getNhanhOrderCode())
                .syncError(order.getSyncError())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
