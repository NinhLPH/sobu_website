package com.vn.sodu.nhanh.service;

import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.NhanhIntegration;
import com.vn.sodu.nhanh.NhanhIntegrationRepo;
import com.vn.sodu.nhanh.dto.*;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhShippingQuoteService {

    private final NhanhClient nhanhClient;
    private final NhanhService nhanhService;
    private final NhanhProperties nhanhProperties;
    private final NhanhIntegrationRepo nhanhIntegrationRepo;

    public List<ShippingQuoteDto> quote(ShippingQuoteRequestDto request) {
        validateRequest(request);
        validateOrigin();

        String accessToken = nhanhService.getValidAccessToken();

        // Load config with database override
        Long standardCarrierId = nhanhProperties.getShipping().getCarrier().getId();
        String standardService = nhanhProperties.getShipping().getCarrier().getStandardService();
        String expressService = nhanhProperties.getShipping().getCarrier().getExpressService();
        Long fallbackId = nhanhProperties.getShipping().getCarrier().getExpressFallbackId();

        java.util.Optional<com.vn.sodu.nhanh.NhanhIntegration> integrationOpt = nhanhService.getIntegration();
        if (integrationOpt.isPresent()) {
            com.vn.sodu.nhanh.NhanhIntegration integration = integrationOpt.get();
            if (integration.getCarrierId() != null) {
                standardCarrierId = integration.getCarrierId();
            }
            if (integration.getStandardService() != null) {
                standardService = integration.getStandardService();
            }
            if (integration.getExpressService() != null) {
                expressService = integration.getExpressService();
            }
            if (integration.getExpressFallbackId() != null) {
                fallbackId = integration.getExpressFallbackId();
            }
        }

        List<ShippingQuoteDto> quotes = new java.util.ArrayList<>();

        // 1. Get Standard Option
        try {
            NhanhResponse<List<NhanhShippingFeeOption>> stdResp = nhanhClient.post(
                    nhanhProperties.getShipping().getFeePath(),
                    accessToken,
                    buildRequestBody(request, standardCarrierId, standardService),
                    new ParameterizedTypeReference<NhanhResponse<List<NhanhShippingFeeOption>>>() {}
            );
            if (stdResp != null && stdResp.getData() != null) {
                stdResp.getData().stream()
                        .filter(java.util.Objects::nonNull)
                        .map(this::toDto)
                        .findFirst()
                        .ifPresent(quotes::add);
            }
        } catch (Exception ex) {
            log.error("Failed to fetch standard shipping quote", ex);
        }

        // 2. Get Express Option
        boolean hasExpress = false;
        if (expressService != null && !expressService.isBlank()) {
            try {
                NhanhResponse<List<NhanhShippingFeeOption>> expResp = nhanhClient.post(
                        nhanhProperties.getShipping().getFeePath(),
                        accessToken,
                        buildRequestBody(request, standardCarrierId, expressService),
                        new ParameterizedTypeReference<NhanhResponse<List<NhanhShippingFeeOption>>>() {}
                );
                if (expResp != null && expResp.getData() != null && !expResp.getData().isEmpty()) {
                    expResp.getData().stream()
                            .filter(java.util.Objects::nonNull)
                            .map(this::toDto)
                            .findFirst()
                            .ifPresent(dto -> {
                                dto.setCarrierServiceName("Hỏa tốc (" + dto.getCarrierServiceName() + ")");
                                quotes.add(dto);
                            });
                    hasExpress = true;
                }
            } catch (Exception ex) {
                log.warn("Express service [{}] failed, will try fallback", expressService, ex);
            }
        }

        // 3. Fallback to expressFallbackId (AhaMove) if no express found yet
        if (!hasExpress) {
            if (fallbackId != null && fallbackId > 0) {
                try {
                    NhanhResponse<List<NhanhShippingFeeOption>> fbResp = nhanhClient.post(
                            nhanhProperties.getShipping().getFeePath(),
                            accessToken,
                            buildRequestBody(request, fallbackId, null),
                            new ParameterizedTypeReference<NhanhResponse<List<NhanhShippingFeeOption>>>() {}
                    );
                    if (fbResp != null && fbResp.getData() != null && !fbResp.getData().isEmpty()) {
                        fbResp.getData().stream()
                                .filter(java.util.Objects::nonNull)
                                .map(this::toDto)
                                .findFirst()
                                .ifPresent(dto -> {
                                    dto.setCarrierServiceName("Hỏa tốc (" + dto.getCarrierName() + ")");
                                    quotes.add(dto);
                                });
                    }
                } catch (Exception ex) {
                    log.error("Failed to fetch express fallback shipping quote", ex);
                }
            }
        }

        if (quotes.isEmpty()) {
            throw new ExternalServiceException("No shipping options available for the selected destination.");
        }

        return quotes;
    }

    // Admin APIs for fetching carriers and saving carrier settings
    public com.fasterxml.jackson.databind.JsonNode getCarriers() {
        String accessToken = nhanhService.getValidAccessToken();
        NhanhResponse<com.fasterxml.jackson.databind.JsonNode> response = nhanhClient.getCarriers(accessToken);
        if (response == null || response.getData() == null) {
            throw new ExternalServiceException("Failed to fetch carriers from Nhanh");
        }
        return response.getData();
    }

    public CarrierConfigResponseDto getCarrierConfig() {
        Long standardCarrierId = nhanhProperties.getShipping().getCarrier().getId();
        String standardService = nhanhProperties.getShipping().getCarrier().getStandardService();
        String expressService = nhanhProperties.getShipping().getCarrier().getExpressService();
        Long fallbackId = nhanhProperties.getShipping().getCarrier().getExpressFallbackId();

        java.util.Optional<com.vn.sodu.nhanh.NhanhIntegration> integrationOpt = nhanhService.getIntegration();
        if (integrationOpt.isPresent()) {
            com.vn.sodu.nhanh.NhanhIntegration integration = integrationOpt.get();
            if (integration.getCarrierId() != null) {
                standardCarrierId = integration.getCarrierId();
            }
            if (integration.getStandardService() != null) {
                standardService = integration.getStandardService();
            }
            if (integration.getExpressService() != null) {
                expressService = integration.getExpressService();
            }
            if (integration.getExpressFallbackId() != null) {
                fallbackId = integration.getExpressFallbackId();
            }
        }

        return CarrierConfigResponseDto.builder()
                .carrierId(standardCarrierId)
                .standardService(standardService)
                .expressService(expressService)
                .expressFallbackId(fallbackId)
                .build();
    }

    public void saveCarrierConfig(CarrierConfigRequestDto request) {
        com.vn.sodu.nhanh.NhanhIntegration integration = nhanhService.getIntegration()
                .orElseThrow(() -> new ExternalServiceException("No Nhanh integration found. Please authenticate first."));

        integration.setCarrierId(request.getCarrierId());
        integration.setStandardService(request.getStandardService());
        integration.setExpressService(request.getExpressService());
        integration.setExpressFallbackId(request.getExpressFallbackId());

        nhanhIntegrationRepo.save(integration);
    }

    private Map<String, Object> buildRequestBody(ShippingQuoteRequestDto request, Long carrierId, String serviceCode) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("type", nhanhProperties.getShipping().getType());
        filters.put("shippingWeight", resolveWeight(request).intValue());
        filters.put("price", money(request.getCartSubtotal()).intValue());
        filters.put("totalCod", money(request.getCodAmount()).intValue());

        if (nhanhProperties.getDepotId() != null && nhanhProperties.getDepotId() > 0) {
            filters.put("depotId", nhanhProperties.getDepotId().intValue());
        }

        filters.put("shippingFrom", shippingFrom());
        filters.put("shippingTo", shippingTo(request));

        if (carrierId != null) {
            Map<String, Object> carrierOpt = new HashMap<>();
            carrierOpt.put("id", carrierId);
            if (serviceCode != null && !serviceCode.isBlank()) {
                carrierOpt.put("service", serviceCode);
            }
            filters.put("carrier", List.of(carrierOpt));
        }

        return Map.of("filters", filters);
    }

    private Map<String, Object> shippingFrom() {
        NhanhProperties.Origin origin = nhanhProperties.getShipping().getOrigin();
        Map<String, Object> value = new HashMap<>();
        value.put("address", origin.getAddress());
        value.put("cityId", origin.getCityId() != null ? origin.getCityId().intValue() : null);
        value.put("districtId", origin.getDistrictId() != null ? origin.getDistrictId().intValue() : null);
        value.put("wardId", origin.getWardId() != null ? origin.getWardId().intValue() : null);
        value.put("locationVersion", "v1");
        return value;
    }

    private Map<String, Object> shippingTo(ShippingQuoteRequestDto request) {
        Map<String, Object> value = new HashMap<>();
        value.put("cityId", request.getCustomerCityId() != null ? request.getCustomerCityId().intValue() : null);
        value.put("districtId", request.getCustomerDistrictId() != null ? request.getCustomerDistrictId().intValue() : null);
        value.put("wardId", request.getCustomerWardId() != null ? request.getCustomerWardId().intValue() : null);
        value.put("locationVersion", "v1");
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
