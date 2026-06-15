package com.vn.sodu.order.mapper;

import com.vn.sodu.order.Order;
import com.vn.sodu.order.dtos.OrderItemResponseDto;
import com.vn.sodu.order.dtos.OrderResponseDto;
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
}
