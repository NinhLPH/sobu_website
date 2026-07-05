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

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        String rawBody = response.getBody();
        if (rawBody == null || rawBody.isBlank()) {
            throw new ExternalServiceException("Nhanh response is null");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (!isSuccessCode(root.path("code"))) {
                throw new ExternalServiceException(responseMessage(root));
            }

            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                data = root;
            }

            String accessToken = textValue(data.get("accessToken"));
            if (accessToken == null || accessToken.isBlank()) {
                throw new ExternalServiceException("Nhanh token response is missing accessToken");
            }

            NhanhTokenResponse result = new NhanhTokenResponse();
            result.setAccessToken(accessToken);

            Long businessId = longValue(data.get("businessId"));
            if (businessId != null) {
                result.setBusinessId(businessId);
            }

            Long expiredAt = longValue(data.get("expiredAt"));
            if (expiredAt != null) {
                result.setExpiredAt(expiredAt);
            }

            return result;
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse Nhanh token response", ex);
            throw new ExternalServiceException("Nhanh token response could not be parsed", ex);
        }
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

    public NhanhResponse<com.fasterxml.jackson.databind.JsonNode> getCarriers(String accessToken) {
        return post(
                "/v3.0/shipping/carrier",
                accessToken,
                Map.of(),
                new ParameterizedTypeReference<NhanhResponse<com.fasterxml.jackson.databind.JsonNode>>() {}
        );
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
        } catch (ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to post data to Nhanh {}", apiPath, ex);
            throw new ExternalServiceException("Nhanh API post failed", ex);
        }
    }

    private String buildApiUrl(String apiPath) {
        return UriComponentsBuilder.fromHttpUrl(nhanhProperties.getBaseUrl())
                .replacePath(apiPath)
                .queryParam("appId", nhanhProperties.getClientId())
                .queryParam("businessId", nhanhProperties.getBusinessId())
                .toUriString();
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

    private <RESP> RESP deserializeOnce(
            String rawBody,
            ParameterizedTypeReference<RESP> responseType,
            String apiPath,
            int httpStatus) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new NhanhApiException(
                    "Nhanh API response is empty",
                    httpStatus,
                    null,
                    null,
                    false,
                    null);
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (!isSuccessCode(root.path("code"))) {
                throw apiException(rawBody, httpStatus, null);
            }
            return objectMapper.readValue(
                    rawBody,
                    objectMapper.getTypeFactory()
                            .constructType(responseType.getType()));
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize Nhanh response for {}: body={}", apiPath, rawBody, ex);
            throw new NhanhApiException(
                    "Nhanh API response could not be parsed",
                    httpStatus,
                    null,
                    null,
                    false,
                    ex);
        }
    }

    private <RESP> RESP deserialize(
            String rawBody,
            ParameterizedTypeReference<RESP> responseType,
            String apiPath) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);

            if (!isSuccessCode(root.path("code"))) {
                String errorCode = textValue(root.get("errorCode"));
                String message = responseMessage(root);
                if ("ERR_429".equals(errorCode)) {
                    throw rateLimitException(root, message);
                }
                if (errorCode == null || errorCode.isBlank()) {
                    throw new ExternalServiceException(message);
                }
                throw new ExternalServiceException(
                        String.format("Nhanh API error [%s]: %s", errorCode, message)
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

    private NhanhApiException apiException(
            String rawBody,
            int httpStatus,
            Throwable cause) {
        if (rawBody == null || rawBody.isBlank()) {
            return new NhanhApiException(
                    "Nhanh API returned HTTP " + httpStatus,
                    httpStatus,
                    null,
                    null,
                    false,
                    cause);
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String errorCode = textValue(root.get("errorCode"));
            String message = responseMessage(root);
            JsonNode data = root.path("data");
            Long unlockedAtEpochSeconds = longValue(data.get("unlockedAt"));
            Instant unlockedAt = unlockedAtEpochSeconds == null
                    ? null
                    : Instant.ofEpochSecond(unlockedAtEpochSeconds);
            return new NhanhApiException(
                    errorCode == null || errorCode.isBlank()
                            ? message
                            : String.format("Nhanh API error [%s]: %s", errorCode, message),
                    httpStatus,
                    errorCode,
                    unlockedAt,
                    false,
                    cause);
        } catch (JsonProcessingException ex) {
            return new NhanhApiException(
                    "Nhanh API returned HTTP " + httpStatus,
                    httpStatus,
                    null,
                    null,
                    false,
                    cause == null ? ex : cause);
        }
    }

    private NhanhRateLimitException rateLimitException(JsonNode root, String message) {
        JsonNode data = root.path("data");
        Long lockedSeconds = longValue(data.get("lockedSeconds"));
        Long unlockedAtEpochSeconds = longValue(data.get("unlockedAt"));
        Instant unlockedAt = unlockedAtEpochSeconds == null ? null : Instant.ofEpochSecond(unlockedAtEpochSeconds);
        return new NhanhRateLimitException(message, lockedSeconds, unlockedAt);
    }

    private boolean isSuccessCode(JsonNode code) {
        if (code == null || code.isMissingNode() || code.isNull()) {
            return false;
        }
        if (code.isBoolean()) {
            return code.booleanValue();
        }
        if (code.isNumber()) {
            return code.asInt() == 1;
        }
        String text = code.asText();
        return "1".equals(text) || "true".equalsIgnoreCase(text);
    }

    private String responseMessage(JsonNode root) {
        JsonNode messages = root.get("messages");
        if (messages != null && messages.isArray()) {
            List<String> values = new ArrayList<>();
            messages.forEach(message -> {
                String value = textValue(message);
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            });
            if (!values.isEmpty()) {
                return String.join("; ", values);
            }
        }

        String message = textValue(messages);
        if (message != null && !message.isBlank()) {
            return message;
        }

        message = textValue(root.get("message"));
        if (message != null && !message.isBlank()) {
            return message;
        }

        return "Nhanh API returned a non-success response";
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.isTextual() ? node.textValue() : node.asText();
    }

    private Long longValue(JsonNode node) {
        String value = textValue(node);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new ExternalServiceException("Nhanh token response contains an invalid numeric value", ex);
        }
    }

    private <T> T withRetry(Supplier<T> supplier, int maxAttempts, long initialDelayMs) {
        int attempt = 0;
        long delay = initialDelayMs;
        while (true) {
            try {
                attempt++;
                return supplier.get();
            } catch (ExternalServiceException ex) {
                throw ex;
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
