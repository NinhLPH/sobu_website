package com.vn.sodu.order.mapper;

import com.vn.sodu.order.*;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class RequestToOrderMapper {

    public Order mapToOrder(Request request, ResolvedOrderCustomer customer) {
        Order order = Order.builder()
                .request(request)
                .orderCode(request.getRequestCode())
                .type(request.getType())
                .status(OrderStatus.NEW)
                .syncStatus(OrderSyncStatus.PENDING)
                .totalAmount(request.getTotalAmount())
                .depositAmount(request.getDepositAmount())
                .description(request.getCustomRequirements())
                .customerMobile(customer.getPhone())
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .customerAddress(customer.getStreet())
                .customerCityName(customer.getProvince())
                .customerDistrictName(customer.getDistrict())
                .customerWardName(customer.getWard())
                .build();

        if (request.getItems() != null) {
            var orderItems = request.getItems().stream()
                    .map(item -> mapToOrderItem(item, order))
                    .collect(Collectors.toList());
            order.setItems(orderItems);
        }

        return order;
    }

    private OrderItem mapToOrderItem(RequestItem requestItem, Order order) {
        return OrderItem.builder()
                .order(order)
                .nhanhProductId(requestItem.getNhanhProductId())
                .name(requestItem.getName())
                .note(requestItem.getNote())
                .price(requestItem.getPrice())
                .quantity(requestItem.getQuantity())
                .build();
    }
}
