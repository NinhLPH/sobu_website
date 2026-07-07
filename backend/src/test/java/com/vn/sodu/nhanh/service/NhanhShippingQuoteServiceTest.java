package com.vn.sodu.nhanh.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.NhanhIntegrationRepo;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.dto.NhanhShippingFeeOption;
import com.vn.sodu.nhanh.dto.ShippingQuoteDto;
import com.vn.sodu.nhanh.dto.ShippingQuoteRequestDto;
import com.vn.sodu.product.dto.NhanhResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NhanhShippingQuoteServiceTest {

    @Mock
    private NhanhClient nhanhClient;

    @Mock
    private NhanhService nhanhService;

    @Mock
    private NhanhIntegrationRepo nhanhIntegrationRepo;

    private NhanhProperties nhanhProperties;
    private NhanhShippingQuoteService service;

    @BeforeEach
    void setUp() {
        nhanhProperties = new NhanhProperties();
        nhanhProperties.getShipping().setFeePath("/v3.0/shipping/fee");
        nhanhProperties.getShipping().setType(1);
        nhanhProperties.getShipping().getOrigin().setAddress("170 De La Thanh");
        nhanhProperties.getShipping().getOrigin().setCityId(254L);
        nhanhProperties.getShipping().getOrigin().setDistrictId(331L);
        nhanhProperties.getShipping().getOrigin().setWardId(1026L);
        nhanhProperties.getLocation().setVersion("v1");

        service = new NhanhShippingQuoteService(
                nhanhClient,
                nhanhService,
                nhanhProperties,
                nhanhIntegrationRepo,
                new ObjectMapper());
    }

    @Test
    void quoteOmitsCarrierFilterForNhanhAccountShipping() {
        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhClient.post(
                eq("/v3.0/shipping/fee"),
                eq("token"),
                any(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(response(option()));

        List<ShippingQuoteDto> quotes = service.quote(request());

        assertEquals(1, quotes.size());
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(nhanhClient).post(
                eq("/v3.0/shipping/fee"),
                eq("token"),
                bodyCaptor.capture(),
                any(ParameterizedTypeReference.class));

        Map<?, ?> filters = (Map<?, ?>) bodyCaptor.getValue().get("filters");
        assertFalse(filters.containsKey("carrier"));
        assertEquals(1, filters.get("type"));
        assertEquals(500, filters.get("shippingWeight"));

        Map<?, ?> shippingTo = (Map<?, ?>) filters.get("shippingTo");
        assertEquals("123 Nguyen Hue", shippingTo.get("address"));
        assertEquals(254, shippingTo.get("cityId"));
        assertEquals(323, shippingTo.get("districtId"));
        assertEquals(1069, shippingTo.get("wardId"));
        assertEquals("v1", shippingTo.get("locationVersion"));
    }

    @Test
    void quoteUsesCarrierObjectForShopAccountSelectedCarrier() {
        nhanhProperties.getShipping().setType(2);
        ShippingQuoteRequestDto request = request();
        request.setCarrierId(8L);
        request.setCarrierServiceId(12L);

        when(nhanhService.getValidAccessToken()).thenReturn("token");
        when(nhanhService.getIntegration()).thenReturn(Optional.empty());
        when(nhanhClient.post(
                eq("/v3.0/shipping/fee"),
                eq("token"),
                any(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(response(option()));

        service.quote(request);

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(nhanhClient).post(
                eq("/v3.0/shipping/fee"),
                eq("token"),
                bodyCaptor.capture(),
                any(ParameterizedTypeReference.class));

        Map<?, ?> filters = (Map<?, ?>) bodyCaptor.getValue().get("filters");
        Object carrier = filters.get("carrier");
        assertInstanceOf(Map.class, carrier);
        assertEquals(8L, ((Map<?, ?>) carrier).get("id"));
        assertEquals("12", ((Map<?, ?>) carrier).get("service"));
    }

    @Test
    void parsesNestedCarrierAndServiceFromNhanhV3FeeResponse() throws Exception {
        String json = """
                {
                  "shipFee": 21000,
                  "customerShipFee": 19000,
                  "carrier": {
                    "id": 29,
                    "name": "Giaohangnhanh"
                  },
                  "service": {
                    "id": 186,
                    "name": "Standard",
                    "description": "Door delivery"
                  }
                }
                """;

        NhanhShippingFeeOption option = new ObjectMapper().readValue(
                json,
                new TypeReference<>() {});

        assertEquals(29L, option.getCarrierId());
        assertEquals("Giaohangnhanh", option.getCarrierName());
        assertEquals(186L, option.getCarrierServiceId());
        assertEquals("Standard", option.getCarrierServiceName());
        assertEquals("Door delivery", option.getDescription());
    }

    private ShippingQuoteRequestDto request() {
        return ShippingQuoteRequestDto.builder()
                .customerAddress("123 Nguyen Hue")
                .customerCityId(254L)
                .customerDistrictId(323L)
                .customerWardId(1069L)
                .cartSubtotal(new BigDecimal("250000.00"))
                .codAmount(BigDecimal.ZERO)
                .shippingWeight(new BigDecimal("500"))
                .build();
    }

    private NhanhShippingFeeOption option() {
        NhanhShippingFeeOption option = new NhanhShippingFeeOption();
        option.setCarrierId(29L);
        option.setCarrierName("Giaohangnhanh");
        option.setCarrierServiceId(186L);
        option.setCarrierServiceName("Standard");
        option.setShipFee(new BigDecimal("21000"));
        option.setCustomerShipFee(new BigDecimal("19000"));
        return option;
    }

    private NhanhResponse<List<NhanhShippingFeeOption>> response(NhanhShippingFeeOption option) {
        return new NhanhResponse<>(1, List.of(option), null);
    }
}
