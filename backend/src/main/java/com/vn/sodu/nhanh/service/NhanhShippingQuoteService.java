package com.vn.sodu.nhanh.service;

import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.dto.NhanhShippingFeeOption;
import com.vn.sodu.nhanh.dto.ShippingQuoteDto;
import com.vn.sodu.nhanh.dto.ShippingQuoteRequestDto;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NhanhShippingQuoteService {

    private final NhanhClient nhanhClient;
    private final NhanhService nhanhService;
    private final NhanhProperties nhanhProperties;

    public List<ShippingQuoteDto> quote(ShippingQuoteRequestDto request) {
        validateRequest(request);
        validateOrigin();

        String accessToken = nhanhService.getValidAccessToken();
        NhanhResponse<List<NhanhShippingFeeOption>> response = nhanhClient.post(
                nhanhProperties.getShipping().getFeePath(),
                accessToken,
                buildRequestBody(request),
                new ParameterizedTypeReference<NhanhResponse<List<NhanhShippingFeeOption>>>() {}
        );

        if (response == null || response.getData() == null) {
            throw new ExternalServiceException("Nhanh shipping fee response is empty");
        }

        return response.getData().stream()
                .filter(option -> option != null)
                .map(this::toDto)
                .toList();
    }

    private Map<String, Object> buildRequestBody(ShippingQuoteRequestDto request) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("type", nhanhProperties.getShipping().getType());
        filters.put("shippingWeight", money(resolveWeight(request)));
        filters.put("price", money(request.getCartSubtotal()));
        filters.put("totalCod", money(request.getCodAmount()));
        filters.put("shippingFrom", shippingFrom());
        filters.put("shippingTo", shippingTo(request));
        if (nhanhProperties.getShipping().getSendCarrierType() != null) {
            filters.put("sendCarrierType", nhanhProperties.getShipping().getSendCarrierType());
        }
        if (request.getCarrierId() != null) {
            filters.put("carrierId", request.getCarrierId());
        }
        if (request.getCarrierServiceId() != null) {
            filters.put("carrierServiceId", request.getCarrierServiceId());
        }
        return Map.of("filters", filters);
    }

    private Map<String, Object> shippingFrom() {
        NhanhProperties.Origin origin = nhanhProperties.getShipping().getOrigin();
        Map<String, Object> value = new HashMap<>();
        value.put("address", origin.getAddress());
        value.put("cityId", origin.getCityId());
        value.put("districtId", origin.getDistrictId());
        value.put("wardId", origin.getWardId());
        return value;
    }

    private Map<String, Object> shippingTo(ShippingQuoteRequestDto request) {
        Map<String, Object> value = new HashMap<>();
        value.put("address", request.getCustomerAddress());
        value.put("cityId", request.getCustomerCityId());
        value.put("districtId", request.getCustomerDistrictId());
        value.put("wardId", request.getCustomerWardId());
        return value;
    }

    private ShippingQuoteDto toDto(NhanhShippingFeeOption option) {
        return ShippingQuoteDto.builder()
                .carrierId(option.getCarrierId())
                .carrierName(option.getCarrierName())
                .carrierServiceId(option.getCarrierServiceId())
                .carrierServiceName(option.getCarrierServiceName())
                .shipFee(money(option.getShipFee()))
                .customerShipFee(money(option.getCustomerShipFee()))
                .deliveryTime(option.getDeliveryTime())
                .description(option.getDescription())
                .build();
    }

    private void validateRequest(ShippingQuoteRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Shipping quote payload is required");
        }
        if (request.getCustomerCityId() == null || request.getCustomerCityId() <= 0
                || request.getCustomerDistrictId() == null || request.getCustomerDistrictId() <= 0
                || request.getCustomerWardId() == null || request.getCustomerWardId() <= 0) {
            throw new IllegalArgumentException("Shipping quote requires customer city, district, and ward ids");
        }
        if (request.getCartSubtotal() == null || request.getCartSubtotal().signum() < 0) {
            throw new IllegalArgumentException("Cart subtotal must be greater than or equal to 0");
        }
        if (request.getCodAmount() != null && request.getCodAmount().signum() < 0) {
            throw new IllegalArgumentException("COD amount must be greater than or equal to 0");
        }
        if (request.getShippingWeight() != null && request.getShippingWeight().signum() <= 0) {
            throw new IllegalArgumentException("Shipping weight must be greater than 0");
        }
    }

    private void validateOrigin() {
        NhanhProperties.Origin origin = nhanhProperties.getShipping().getOrigin();
        if (origin == null
                || origin.getCityId() == null
                || origin.getCityId() <= 0
                || origin.getDistrictId() == null
                || origin.getDistrictId() <= 0
                || origin.getWardId() == null
                || origin.getWardId() <= 0) {
            throw new IllegalStateException("Nhanh shipping origin city, district, and ward ids are not configured");
        }
    }

    private BigDecimal resolveWeight(ShippingQuoteRequestDto request) {
        if (request.getShippingWeight() != null) {
            return request.getShippingWeight();
        }
        if (nhanhProperties.getShipping().getDefaults() == null
                || nhanhProperties.getShipping().getDefaults().getWeight() == null) {
            return BigDecimal.ONE;
        }
        return nhanhProperties.getShipping().getDefaults().getWeight();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
