package com.vn.sodu.nhanh.service;

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
import org.springframework.web.util.UriComponentsBuilder;

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
                resp = withRetry(call, 3, 500);
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

        String url = UriComponentsBuilder.fromHttpUrl(nhanhProperties.getBaseUrl())
                .replacePath(apiPath)
                .queryParam("appId", nhanhProperties.getClientId())
                .queryParam("businessId", nhanhProperties.getBusinessId())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);

        HttpEntity<REQ> request = new HttpEntity<>(body, headers);

        Supplier<RESP> call = () -> {
            ResponseEntity<RESP> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            request,
                            responseType
                    );
            return response.getBody();
        };

        try {
            return withRetry(call, 3, 500);
        } catch (Exception ex) {
            log.error("Failed to post data to Nhanh {}", apiPath, ex);
            throw new ExternalServiceException("Nhanh API post failed", ex);
        }
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
