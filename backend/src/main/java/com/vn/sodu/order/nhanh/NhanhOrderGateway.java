package com.vn.sodu.order.nhanh;

import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.dto.NhanhOrderAddRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.nhanh.dto.NhanhOrderEditRequest;
import com.vn.sodu.nhanh.dto.NhanhOrderEditResult;
import com.vn.sodu.nhanh.service.NhanhClient;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderItem;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentType;
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
    private static final String ORDER_EDIT_PATH = "/v3.0/order/edit";
    private static final int SHOPPING_ORDER_TYPE = 2;
    private static final String SOURCE_NAME = "Sodu Website";

    private final NhanhClient nhanhClient;
    private final NhanhProperties nhanhProperties;

    public NhanhOrderAddResult createOrder(NhanhOrderAddRequest request, String accessToken) {
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

    public NhanhOrderEditResult editOrder(NhanhOrderEditRequest request, String accessToken) {
        NhanhResponse<List<NhanhOrderEditResult>> response = nhanhClient.post(
                ORDER_EDIT_PATH,
                accessToken,
                request,
                new ParameterizedTypeReference<NhanhResponse<List<NhanhOrderEditResult>>>() {}
        );

        if (response == null) {
            throw new ExternalServiceException("Nhanh order edit response is null");
        }
        if (response.getCode() == 1) {
            List<NhanhOrderEditResult> data = response.getData();
            if (data == null || data.isEmpty()) {
                return new NhanhOrderEditResult();
            }
            return data.get(0);
        }
        throw new ExternalServiceException(errorMessage(response));
    }

    public NhanhOrderAddRequest buildAddRequest(Order order, OrderPayment payment) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required");
        }
        if (payment == null) {
            throw new IllegalArgumentException("Payment is required");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item to sync to Nhanh");
        }
        requireShipping(order);
        Long accountId = requireAccountingAccountId();

        List<NhanhOrderAddRequest.Product> products = order.getItems().stream()
                .map(this::toProduct)
                .toList();

        NhanhOrderAddRequest.Payment.PaymentBuilder paymentBuilder = NhanhOrderAddRequest.Payment.builder();
        if (order.getType() == com.vn.sodu.request.OrderType.PREORDER && payment.getType() == PaymentType.DEPOSIT) {
            paymentBuilder
                    .depositAmount(defaultMoney(payment.getAmount()))
                    .depositAccountId(accountId);
        } else {
            paymentBuilder
                    .transferAmount(defaultMoney(payment.getAmount()))
                    .transferAccountId(accountId);
        }

        return NhanhOrderAddRequest.builder()
                .info(NhanhOrderAddRequest.Info.builder()
                        .depotId(nhanhProperties.getDepotId())
                        .type(SHOPPING_ORDER_TYPE)
                        .description(order.getDescription())
                        .build())
                .channel(NhanhOrderAddRequest.Channel.builder()
                        .appOrderId(defaultText(order.getAppOrderId(), order.getOrderCode()))
                        .sourceName(SOURCE_NAME)
                        .build())
                .shippingAddress(NhanhOrderAddRequest.ShippingAddress.builder()
                        .name(order.getCustomerName())
                        .mobile(order.getCustomerMobile())
                        .email(order.getCustomerEmail())
                        .address(order.getCustomerAddress())
                        .cityId(order.getCustomerCityId())
                        .districtId(order.getCustomerDistrictId())
                        .wardId(order.getCustomerWardId())
                        .locationVersion(defaultText(order.getLocationVersion(), "v1"))
                        .build())
                .products(products)
                .carrier(NhanhOrderAddRequest.Carrier.builder()
                        .id(order.getCarrierId())
                        .serviceId(order.getCarrierServiceId())
                        .customerShipFee(defaultMoney(order.getShippingFee()))
                        .build())
                .payment(paymentBuilder.build())
                .build();
    }

    public NhanhOrderEditRequest buildEditRequest(Order order, OrderPayment payment) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required");
        }
        if (payment == null) {
            throw new IllegalArgumentException("Payment is required");
        }
        if (order.getNhanhOrderId() == null || order.getNhanhOrderId().isBlank()) {
            throw new IllegalArgumentException("Nhanh order id is required for order edit");
        }
        requireShipping(order);

        return NhanhOrderEditRequest.builder()
                .info(NhanhOrderEditRequest.Info.builder()
                        .id(parseLong(order.getNhanhOrderId(), "Nhanh order id"))
                        .description(order.getDescription())
                        .build())
                .carrier(NhanhOrderEditRequest.Carrier.builder()
                        .id(order.getCarrierId())
                        .serviceId(order.getCarrierServiceId())
                        .customerShipFee(defaultMoney(order.getShippingFee()))
                        .build())
                .payment(NhanhOrderEditRequest.Payment.builder()
                        .transferAmount(defaultMoney(payment.getAmount()))
                        .transferAccountId(requireAccountingAccountId())
                        .code(payment.getPaymentCode())
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

    private void requireShipping(Order order) {
        if (order.getCustomerCityId() == null || order.getCustomerDistrictId() == null || order.getCustomerWardId() == null) {
            throw new IllegalArgumentException("Nhanh sync requires customer city, district, and ward ids");
        }
        if (order.getCarrierId() == null || order.getCarrierServiceId() == null) {
            throw new IllegalArgumentException("Nhanh sync requires carrier id and carrier service id");
        }
    }

    private Long requireAccountingAccountId() {
        if (nhanhProperties.getAccounting() == null || nhanhProperties.getAccounting().getAccountId() == null) {
            throw new IllegalStateException("Nhanh accounting account id is not configured");
        }
        return nhanhProperties.getAccounting().getAccountId();
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String defaultText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private Long parseLong(String value, String label) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be numeric");
        }
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
