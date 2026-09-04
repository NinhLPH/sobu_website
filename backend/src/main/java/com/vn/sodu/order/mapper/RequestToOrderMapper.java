package com.vn.sodu.order.mapper;

import com.vn.sodu.order.*;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.request.Request;
import com.vn.sodu.request.RequestItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RequestToOrderMapper {

    private final ProductRepo productRepo;

    public Order mapToOrder(Request request, ResolvedOrderCustomer customer) {
        Order order = Order.builder()
                .request(request)
                .orderCode(request.getRequestCode())
                .appOrderId(request.getRequestCode())
                .type(request.getType())
                .status(OrderStatus.NEW)
                .syncStatus(OrderSyncStatus.PENDING)
                .nhanhSyncStage(NhanhSyncStage.NONE)
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
                .customerCityId(customer.getProvinceId())
                .customerDistrictId(customer.getDistrictId())
                .customerWardId(customer.getWardId())
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
                .productId(resolveProductId(requestItem.getNhanhProductId()))
                .nhanhProductId(requestItem.getNhanhProductId())
                .name(requestItem.getName())
                .note(requestItem.getNote())
                .price(requestItem.getPrice())
                .quantity(requestItem.getQuantity())
                .build();
    }

    private Long resolveProductId(String nhanhProductId) {
        if (nhanhProductId == null || nhanhProductId.isBlank()) {
            return null;
        }
        try {
            return productRepo.findByExternalId(Long.parseLong(nhanhProductId))
                    .map(Product::getId)
                    .orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
