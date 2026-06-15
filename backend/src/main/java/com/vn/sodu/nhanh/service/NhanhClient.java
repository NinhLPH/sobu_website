package com.vn.sodu.nhanh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.NhanhTokenResponse;
import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhClient {

    private final RestTemplate restTemplate;
    private final NhanhProperties nhanhProperties;
    private final ObjectMapper objectMapper;

    private static final String ACCESS_TOKEN_URL =
            "https://pos.open.nhanh.vn/v3.0/app/getaccesstoken?appId=%s";

    public NhanhTokenResponse getAccessToken(String accessCode) {

        String url = String.format(ACCESS_TOKEN_URL, nhanhProperties.getClientId());

        Map<String, Object> body = Map.of(
                "accessCode", accessCode,
                "secretKey", nhanhProperties.getClientSecret()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        Map<String, Object> resp = response.getBody();

        if (resp == null) {
            throw new ExternalServiceException("Nhanh response is null");
        }

        // Removed raw logging to prevent leak
        
        Object code = resp.get("code");
        if (code == null || Integer.parseInt(code.toString()) != 1) {
            throw new ExternalServiceException("Nhanh API returned a non-success response");
        }

        Map<String, Object> data;
        if (resp.containsKey("data") && resp.get("data") instanceof Map) {
            data = (Map<String, Object>) resp.get("data");
        } else {
            data = resp;
        }

        if (data.get("accessToken") == null) {
            throw new ExternalServiceException("Nhanh token response is missing accessToken");
        }

        NhanhTokenResponse result = new NhanhTokenResponse();
        result.setAccessToken((String) data.get("accessToken"));

        if (data.get("businessId") != null) {
            result.setBusinessId(Long.valueOf(data.get("businessId").toString()));
        }

        if (data.get("expiredAt") != null) {
            result.setExpiredAt(Long.valueOf(data.get("expiredAt").toString()));
        }

        return result;
    }

    public <T> List<T> fetchAllPages(
            String apiPath,
            String accessToken,
            Map<String, Object> baseFilters,
            ParameterizedTypeReference<NhanhResponse<List<T>>> responseType) {

        List<T> result = new ArrayList<>();
        Object nextCursor = null;
        int requestCount = 0;

        String url = UriComponentsBuilder.fromHttpUrl(nhanhProperties.getBaseUrl())
                .replacePath(apiPath)
                .queryParam("appId", nhanhProperties.getClientId())
                .queryParam("businessId", nhanhProperties.getBusinessId())
                .toUriString();

        while (true) {
            requestCount++;
            
            Map<String, Object> body = new HashMap<>();
            if (baseFilters != null && !baseFilters.isEmpty()) {
                body.put("filters", baseFilters);
            }
            
            Map<String, Object> paginator = new HashMap<>();
            paginator.put("size", 50);
            if (nextCursor != null) {
                paginator.put("next", nextCursor);
            }
            body.put("paginator", paginator);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            Supplier<NhanhResponse<List<T>>> call = () -> {
                ResponseEntity<NhanhResponse<List<T>>> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                request,
                                responseType
                        );
                return response.getBody();
            };

            NhanhResponse<List<T>> resp;
            try {
                resp = withRetry(call, 3, 43000 );
            } catch (Exception ex) {
                log.error("Failed to fetch data from Nhanh {} at request={}", apiPath, requestCount, ex);
                throw new ExternalServiceException("Nhanh API fetch failed", ex);
            }

            if (resp == null) {
                log.error("Nhanh response is null at request={}", requestCount);
                throw new ExternalServiceException("Nhanh response is null");
            }

            if (resp.getCode() != 1) {
                String message = errorMessage(resp);
                log.error("Nhanh API returned non-success code at request={}: {}", requestCount, resp.getCode());
                throw new ExternalServiceException(message);
            }

            if (resp.getData() == null) {
                log.error("Nhanh response data is null at request={}", requestCount);
                throw new ExternalServiceException("Nhanh response data is null");
            }

            List<T> items = resp.getData();
            if (items.isEmpty()) {
                break;
            }

            result.addAll(items);

            if (resp.getPaginator() == null || resp.getPaginator().getNext() == null) {
                break;
            }

            nextCursor = resp.getPaginator().getNext();
        }
        return result;
    }

    private String errorMessage(NhanhResponse<?> response) {
        if (response.getMessages() == null || response.getMessages().isEmpty()) {
            return "Nhanh API returned a non-success response";
        }
        return String.join("; ", response.getMessages());
    }

    public <REQ, RESP> RESP post(
            String apiPath,
            String accessToken,
            REQ body,
            ParameterizedTypeReference<RESP> responseType) {
        return postWithAuthorization(apiPath, accessToken, body, responseType);
    }

    public <REQ, RESP> RESP postOnce(
            String apiPath,
            String accessToken,
            REQ body,
            ParameterizedTypeReference<RESP> responseType) {
        String url = buildApiUrl(apiPath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);
            return deserializeOnce(response.getBody(), responseType, apiPath, response.getStatusCode().value());
        } catch (RestClientResponseException ex) {
            throw apiException(ex.getResponseBodyAsString(), ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            throw new NhanhApiException(
                    "Nhanh API transport failure",
                    null,
                    null,
                    null,
                    true,
                    ex);
        }
    }

    public <REQ, RESP> RESP postWithBearerAuthorization(
            String apiPath,
            String accessToken,
            REQ body,
            ParameterizedTypeReference<RESP> responseType) {
        return postWithAuthorization(apiPath, bearer(accessToken), body, responseType);
    }

    private <REQ, RESP> RESP postWithAuthorization(
            String apiPath,
            String authorizationHeader,
            REQ body,
            ParameterizedTypeReference<RESP> responseType) {

        String url = buildApiUrl(apiPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", authorizationHeader);

        HttpEntity<REQ> request = new HttpEntity<>(body, headers);

        Supplier<RESP> call = () -> {
            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            request,
                            String.class
                    );
            String rawBody = response.getBody();
            logOrderAddRawResponse(apiPath, rawBody);
            if (rawBody == null || rawBody.isBlank()) {
                return null;
            }
            return deserialize(rawBody, responseType, apiPath);
        };

        try {
            return withRetry(call, 3, 500);
        } catch (Exception ex) {
            log.error("Failed to post data to Nhanh {}", apiPath, ex);
            throw new ExternalServiceException("Nhanh API post failed", ex);
        }
    }

    private String bearer(String accessToken) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            return accessToken;
        }
        return "Bearer " + accessToken;
    }

    private void logOrderAddRawResponse(String apiPath, String rawBody) {
        if (!"/v3.0/order/add".equals(apiPath)) {
            return;
        }
        if (rawBody == null || rawBody.isBlank()) {
            log.warn("Nhanh order add raw response body is empty");
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode firstData = root.path("data").isArray() && root.path("data").size() > 0
                    ? root.path("data").get(0)
                    : null;
            String rawDataOrderId = textValue(firstData == null ? null : firstData.get("orderId"));
            String rawDataId = textValue(firstData == null ? null : firstData.get("id"));

            if (rawDataOrderId == null) {
                log.warn(
                        "Nhanh order add raw response before parse missing data[0].orderId: dataId={}, body={}",
                        rawDataId,
                        rawBody
                );
                return;
            }

            log.info(
                    "Nhanh order add raw response before parse: dataOrderId={}, dataId={}, body={}",
                    rawDataOrderId,
                    rawDataId,
                    rawBody
            );
        } catch (JsonProcessingException ex) {
            log.warn("Nhanh order add raw response could not be inspected before parse: body={}", rawBody, ex);
        }
    }

    private <RESP> RESP deserialize(
            String rawBody,
            ParameterizedTypeReference<RESP> responseType,
            String apiPath) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);

            int code = root.path("code").asInt();

            if (code != 1) {

                String errorCode =
                        root.path("errorCode").asText();

                String message =
                        root.path("messages").asText();

                throw new ExternalServiceException(
                        String.format(
                                "Nhanh API error [%s]: %s",
                                errorCode,
                                message
                        )
                );
            }
            return objectMapper.readValue(
                    rawBody,
                    objectMapper.getTypeFactory()
                            .constructType(responseType.getType())
            );
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize Nhanh response for {}: body={}", apiPath, rawBody, ex);
            throw new ExternalServiceException("Nhanh API response could not be parsed", ex);
        }
    }

    private <RESP> RESP deserializeOnce(
            String rawBody,
            ParameterizedTypeReference<RESP> responseType,
            String apiPath,
            int httpStatus) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new NhanhApiException(
                    "Nhanh API returned an empty response",
                    httpStatus,
                    null,
                    null,
                    false,
                    null);
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (root.path("code").asInt() != 1) {
                throw apiException(root, httpStatus, null);
            }
            return objectMapper.readValue(
                    rawBody,
                    objectMapper.getTypeFactory().constructType(responseType.getType()));
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize Nhanh response for {}", apiPath, ex);
            throw new NhanhApiException(
                    "Nhanh API response could not be parsed",
                    httpStatus,
                    null,
                    null,
                    false,
                    ex);
        }
    }

    private NhanhApiException apiException(String rawBody, int httpStatus, Throwable cause) {
        if (rawBody == null || rawBody.isBlank()) {
            return new NhanhApiException(
                    "Nhanh API request failed",
                    httpStatus,
                    null,
                    null,
                    false,
                    cause);
        }
        try {
            return apiException(objectMapper.readTree(rawBody), httpStatus, cause);
        } catch (JsonProcessingException ex) {
            return new NhanhApiException(
                    "Nhanh API request failed",
                    httpStatus,
                    null,
                    null,
                    false,
                    cause);
        }
    }

    private NhanhApiException apiException(JsonNode root, int httpStatus, Throwable cause) {
        String errorCode = firstText(root, "errorCode", "codeError");
        Instant unlockedAt = parseInstant(findField(root, "unlockedAt"));
        String message = firstText(root, "message", "error", "description");
        if (message == null) {
            JsonNode messages = root.get("messages");
            message = messages == null ? null : messages.toString();
        }
        if (message == null || message.isBlank()) {
            message = "Nhanh API request failed";
        }
        return new NhanhApiException(
                message,
                httpStatus,
                errorCode,
                unlockedAt,
                false,
                cause);
    }

    private String buildApiUrl(String apiPath) {
        return UriComponentsBuilder.fromHttpUrl(nhanhProperties.getBaseUrl())
                .replacePath(apiPath)
                .queryParam("appId", nhanhProperties.getClientId())
                .queryParam("businessId", nhanhProperties.getBusinessId())
                .toUriString();
    }

    private JsonNode findField(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode direct = node.get(fieldName);
        if (direct != null) {
            return direct;
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                JsonNode found = findField(child, fieldName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = findField(node, fieldName);
            if (value != null && !value.isNull() && !value.isContainerNode()) {
                String text = value.asText();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private Instant parseInstant(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            if (node.isNumber()) {
                long value = node.asLong();
                return Math.abs(value) >= 1_000_000_000_000L
                        ? Instant.ofEpochMilli(value)
                        : Instant.ofEpochSecond(value);
            }
            String value = node.asText();
            if (value.matches("-?\\d+")) {
                long epoch = Long.parseLong(value);
                return Math.abs(epoch) >= 1_000_000_000_000L
                        ? Instant.ofEpochMilli(epoch)
                        : Instant.ofEpochSecond(epoch);
            }
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.isTextual() ? node.textValue() : node.asText();
    }

    private <T> T withRetry(Supplier<T> supplier, int maxAttempts, long initialDelayMs) {
        int attempt = 0;
        long delay = initialDelayMs;
        while (true) {
            try {
                attempt++;
                return supplier.get();
            } catch (Exception ex) {
                if (attempt >= maxAttempts) {
                    log.error("Operation failed after {} attempts", attempt, ex);
                    throw ex;
                }
                log.warn("Operation failed on attempt {} - retrying after {}ms", attempt, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ExternalServiceException("Nhanh retry interrupted", ie);
                }
                delay *= 2;
            }
        }
    }
}
