package com.vn.sodu.nhanh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.dto.NhanhOrderAddResult;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.dto.NhanhResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NhanhClientTest {

    private RestTemplate restTemplate;
    private NhanhClient nhanhClient;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);

        NhanhProperties properties = new NhanhProperties();
        properties.setBaseUrl("https://pos.open.nhanh.vn/api");
        properties.setClientId("77323");
        properties.setClientSecret("secret");
        properties.setRedirectUri("http://localhost/callback");
        properties.setBusinessId("224003");

        nhanhClient = new NhanhClient(restTemplate, properties, new ObjectMapper());
    }

    @Test
    @DisplayName("Should surface expired access code message from token exchange")
    void testGetAccessTokenExpiredAccessCodeResponse() {
        String rawResponse = """
                {
                  "code": false,
                  "messages": [
                    "Invalid accessCode or accessCode has expired"
                  ]
                }
                """;

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(rawResponse));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class, () ->
                nhanhClient.getAccessToken("expired-access-code"));

        assertEquals("Invalid accessCode or accessCode has expired", ex.getMessage());
    }

    @Test
    @DisplayName("Should expose Nhanh rate limit metadata")
    void testPostRateLimitResponseIncludesUnlockMetadata() {
        String rawResponse = """
                {
                  "code": 0,
                  "errorCode": "ERR_429",
                  "message": "Your app exceeded the API Rate Limit",
                  "data": {
                    "lockedSeconds": 10,
                    "unlockedAt": 1733387520
                  }
                }
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(rawResponse));

        NhanhRateLimitException ex = assertThrows(NhanhRateLimitException.class, () -> nhanhClient.post(
                "/v3.0/shipping/location",
                "token",
                Map.of("filters", Map.of("locationVersion", "v1", "type", "CITY")),
                new ParameterizedTypeReference<NhanhResponse<List<Map<String, Object>>>>() {}
        ));

        assertEquals("Your app exceeded the API Rate Limit", ex.getMessage());
        assertEquals(10L, ex.getLockedSeconds());
        assertEquals(1733387520L, ex.getUnlockedAt().getEpochSecond());
    }

    @Test
    @DisplayName("Should build list request with app, business, filters, and paginator")
    void testFetchAllPagesRequestShape() {
        NhanhProductDTO dto = new NhanhProductDTO();
        dto.setId(1L);
        NhanhResponse<List<NhanhProductDTO>> response =
                new NhanhResponse<>(1, List.of(dto), null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(response));

        List<NhanhProductDTO> result = nhanhClient.fetchAllPages(
                "/v3.0/product/list",
                "token",
                Map.of("updatedAtFrom", 1704067200L),
                new ParameterizedTypeReference<NhanhResponse<List<NhanhProductDTO>>>() {}
        );

        assertEquals(1, result.size());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.POST), requestCaptor.capture(), any(ParameterizedTypeReference.class));

        assertEquals("https://pos.open.nhanh.vn/v3.0/product/list?appId=77323&businessId=224003", urlCaptor.getValue());
        assertEquals("token", requestCaptor.getValue().getHeaders().getFirst("Authorization"));

        Map<?, ?> body = (Map<?, ?>) requestCaptor.getValue().getBody();
        assertEquals(Map.of("updatedAtFrom", 1704067200L), body.get("filters"));
        Map<?, ?> paginator = (Map<?, ?>) body.get("paginator");
        assertEquals(50, paginator.get("size"));
        assertFalse(paginator.containsKey("next"));
    }

    @Test
    @DisplayName("Should follow next cursor across pages")
    void testFetchAllPagesUsesNextCursor() {
        NhanhProductDTO first = new NhanhProductDTO();
        first.setId(1L);
        NhanhProductDTO second = new NhanhProductDTO();
        second.setId(2L);

        NhanhResponse<List<NhanhProductDTO>> page1 =
                new NhanhResponse<>(1, List.of(first), new NhanhResponse.Paginator("cursor-2"));
        NhanhResponse<List<NhanhProductDTO>> page2 =
                new NhanhResponse<>(1, List.of(second), null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(page1))
                .thenReturn(ResponseEntity.ok(page2));

        List<NhanhProductDTO> result = nhanhClient.fetchAllPages(
                "/v3.0/product/list",
                "token",
                null,
                new ParameterizedTypeReference<NhanhResponse<List<NhanhProductDTO>>>() {}
        );

        assertEquals(2, result.size());

        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), requestCaptor.capture(), any(ParameterizedTypeReference.class));
        Map<?, ?> secondBody = (Map<?, ?>) requestCaptor.getAllValues().get(1).getBody();
        Map<?, ?> secondPaginator = (Map<?, ?>) secondBody.get("paginator");
        assertEquals("cursor-2", secondPaginator.get("next"));
    }

    @Test
    @DisplayName("Should retry transient failures")
    void testFetchAllPagesRetries() {
        NhanhProductDTO dto = new NhanhProductDTO();
        dto.setId(1L);
        NhanhResponse<List<NhanhProductDTO>> response =
                new NhanhResponse<>(1, List.of(dto), null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("temporary"))
                .thenReturn(ResponseEntity.ok(response));

        List<NhanhProductDTO> result = nhanhClient.fetchAllPages(
                "/v3.0/product/list",
                "token",
                null,
                new ParameterizedTypeReference<NhanhResponse<List<NhanhProductDTO>>>() {}
        );

        assertEquals(1, result.size());
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should throw sanitized error for upstream failure response")
    void testFetchAllPagesSanitizesErrorResponse() {
        NhanhResponse<List<NhanhProductDTO>> response =
                new NhanhResponse<>(0, null, null);
        response.setMessages(List.of("Invalid accessToken or accessToken has expired"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(response));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class, () -> nhanhClient.fetchAllPages(
                "/v3.0/product/list",
                "token",
                null,
                new ParameterizedTypeReference<NhanhResponse<List<NhanhProductDTO>>>() {}
        ));

        assertEquals("Invalid accessToken or accessToken has expired", ex.getMessage());
        assertFalse(ex.getMessage().contains("code=0"));
    }

    @Test
    @DisplayName("Should deserialize typed post response from raw JSON body")
    void testPostDeserializesRawJsonResponse() {
        String rawResponse = """
                {
                  "code": 1,
                  "data": {
                    "orderId": 654321,
                    "trackingUrl": "https://track.example/order/654321"
                  }
                }
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(rawResponse));

        NhanhResponse<NhanhOrderAddResult> response = nhanhClient.post(
                "/v3.0/order/add",
                "token",
                Map.of("sample", "payload"),
                new ParameterizedTypeReference<NhanhResponse<NhanhOrderAddResult>>() {}
        );

        assertNotNull(response);
        assertEquals(1, response.getCode());
        assertNotNull(response.getData());
        assertEquals(654321L, response.getData().getOrderId());
        assertEquals("654321", response.getData().resolveNhanhOrderId());
    }

    @Test
    @DisplayName("Should build raw-token authorized location request")
    void testPostLocationRequestShape() {
        String rawResponse = """
                {
                  "code": 1,
                  "data": [
                    {
                      "id": 254,
                      "name": "Ha Noi"
                    }
                  ]
                }
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(rawResponse));

        NhanhResponse<List<Map<String, Object>>> response = nhanhClient.post(
                "/v3.0/shipping/location",
                "token",
                Map.of("filters", Map.of(
                        "locationVersion", "v1",
                        "type", "CITY"
                )),
                new ParameterizedTypeReference<NhanhResponse<List<Map<String, Object>>>>() {}
        );

        assertNotNull(response);
        assertEquals(1, response.getCode());
        assertEquals(1, response.getData().size());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.POST), requestCaptor.capture(), eq(String.class));

        assertEquals("https://pos.open.nhanh.vn/v3.0/shipping/location?appId=77323&businessId=224003", urlCaptor.getValue());
        assertEquals("token", requestCaptor.getValue().getHeaders().getFirst("Authorization"));
        Map<?, ?> body = (Map<?, ?>) requestCaptor.getValue().getBody();
        Map<?, ?> filters = (Map<?, ?>) body.get("filters");
        assertEquals("v1", filters.get("locationVersion"));
        assertEquals("CITY", filters.get("type"));
    }

    @Test
    @DisplayName("Should surface expired access token message from boolean false response")
    void testPostLocationExpiredAccessTokenResponse() {
        String rawResponse = """
                {
                  "code": false,
                  "messages": [
                    "Invalid accessToken or accessToken has expired"
                  ]
                }
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(rawResponse));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class, () -> nhanhClient.post(
                "/v3.0/shipping/location",
                "expired-token",
                Map.of("filters", Map.of(
                        "locationVersion", "v1",
                        "type", "CITY"
                )),
                new ParameterizedTypeReference<NhanhResponse<List<Map<String, Object>>>>() {}
        ));

        assertEquals("Invalid accessToken or accessToken has expired", ex.getMessage());
        verify(restTemplate, times(1))
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }
}
