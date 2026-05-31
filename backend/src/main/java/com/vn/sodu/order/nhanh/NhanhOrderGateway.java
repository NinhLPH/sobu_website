package com.vn.sodu.order.nhanh;

import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.dto.NhanhOrderAddRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.nhanh.service.NhanhClient;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class NhanhOrderGateway {

    private static final String ORDER_ADD_PATH = "/v3.0/order/add";
    private static final int SHOPPING_ORDER_TYPE = 2;
    private static final String SOURCE_NAME = "Sodu Website";

    private final NhanhClient nhanhClient;

    public NhanhOrderAddResult createOrder(Order order, String accessToken) {
        NhanhOrderAddRequest request = toNhanhOrderAddRequest(order);
        NhanhResponse<List<NhanhOrderAddResult>> response = nhanhClient.post(
                ORDER_ADD_PATH,
                accessToken,
                request,
                new ParameterizedTypeReference<NhanhResponse<List<NhanhOrderAddResult>>>() {}
        );

        if (response == null) {
            throw new ExternalServiceException("Nhanh order add response is null");
        }
        if (response.getCode() == 1) {
            List<NhanhOrderAddResult> data = response.getData();
            if (data == null || data.isEmpty()) {
                return new NhanhOrderAddResult();
            }
            return data.get(0);
        }
        if (isDuplicateResponse(response)) {
            return NhanhOrderAddResult.duplicate();
        }
        throw new ExternalServiceException(errorMessage(response));
    }

    NhanhOrderAddRequest toNhanhOrderAddRequest(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item to sync to Nhanh");
        }

        List<NhanhOrderAddRequest.Product> products = order.getItems().stream()
                .map(this::toProduct)
                .toList();

        return NhanhOrderAddRequest.builder()
                .info(NhanhOrderAddRequest.Info.builder()
                        .type(SHOPPING_ORDER_TYPE)
                        .description(order.getDescription())
                        .build())
                .channel(NhanhOrderAddRequest.Channel.builder()
                        .appOrderId(order.getOrderCode())
                        .sourceName(SOURCE_NAME)
                        .build())
                .shippingAddress(NhanhOrderAddRequest.ShippingAddress.builder()
                        .name(order.getCustomerName())
                        .mobile(order.getCustomerMobile())
                        .email(order.getCustomerEmail())
                        .address(order.getCustomerAddress())
                        .build())
                .products(products)
                .payment(NhanhOrderAddRequest.Payment.builder()
                        .depositAmount(defaultMoney(order.getDepositAmount()))
                        .build())
                .build();
    }

    private NhanhOrderAddRequest.Product toProduct(OrderItem item) {
        if (item.getNhanhProductId() == null || item.getNhanhProductId().isBlank()) {
            throw new IllegalArgumentException("Order item is missing Nhanh product id");
        }
        return NhanhOrderAddRequest.Product.builder()
                .id(Long.valueOf(item.getNhanhProductId()))
                .price(defaultMoney(item.getPrice()))
                .quantity(item.getQuantity())
                .discount(defaultMoney(item.getDiscount()))
                .description(item.getNote())
                .build();
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isDuplicateResponse(NhanhResponse<?> response) {
        String message = errorMessage(response).toLowerCase(Locale.ROOT);
        return message.contains("duplicate")
                || message.contains("already exists")
                || message.contains("trung")
                || message.contains("trung lap")
                || message.contains("trùng")
                || message.contains("tồn tại")
                || message.contains("ton tai")
                || message.contains("apporderid");
    }

    private String errorMessage(NhanhResponse<?> response) {
        if (response.getMessages() == null || response.getMessages().isEmpty()) {
            return "Nhanh order add failed";
        }
        return String.join("; ", response.getMessages());
    }
}
