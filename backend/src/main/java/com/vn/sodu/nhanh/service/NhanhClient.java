package com.vn.sodu.nhanh.service;

import com.vn.sodu.nhanh.NhanhTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nhanh.client-id}")
    private String clientId;

    @Value("${nhanh.client-secret}")
    private String clientSecret;

    private static final String URL =
            "https://pos.open.nhanh.vn/v3.0/app/getaccesstoken?appId=%s";

    public NhanhTokenResponse getAccessToken(String accessCode) {

        String url = String.format(URL, clientId);

        // body JSON theo doc v3
        Map<String, Object> body = Map.of(
                "accessCode", accessCode,
                "secretKey", clientSecret
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, request, Map.class);

        Map<String, Object> resp = response.getBody();

        if (resp == null) {
            throw new RuntimeException("Nhanh response is null");
        }

        log.info("Nhanh raw response: {}", resp);

        // check code
        Object code = resp.get("code");
        if (code == null || Integer.parseInt(code.toString()) != 1) {
            throw new RuntimeException("Nhanh API error: " + resp);
        }

        // HANDLE 2 CASE:
        // 1. có "data"
        // 2. flatten (không có data)

        Map<String, Object> data;

        if (resp.containsKey("data") && resp.get("data") instanceof Map) {
            data = (Map<String, Object>) resp.get("data");
        } else {
            // fallback: dùng luôn resp
            data = resp;
        }

        if (data.get("accessToken") == null) {
            throw new RuntimeException("Missing accessToken: " + resp);
        }

        NhanhTokenResponse result = new NhanhTokenResponse();

        result.setAccessToken((String) data.get("accessToken"));

        if (data.get("businessId") != null) {
            result.setBusinessId(
                    Long.valueOf(data.get("businessId").toString())
            );
        }

        if (data.get("expiredAt") != null) {
            result.setExpiredAt(
                    Long.valueOf(data.get("expiredAt").toString())
            );
        }

        return result;
    }
}