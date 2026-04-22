package com.vn.sodu.nhanh.service;

import com.vn.sodu.nhanh.NhanhTokenApiResponse;
import com.vn.sodu.nhanh.NhanhTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NhanhClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nhanh.base-url}")
    private String baseUrl;

    @Value("${nhanh.client-id}")
    private String clientId;

    @Value("${nhanh.client-secret}")
    private String clientSecret;

    public NhanhTokenResponse getAccessToken(String accessCode) {

        String url = "https://pos.open.nhanh.vn/v3.0/app/getaccesstoken?appId=" + clientId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("accessCode", accessCode);
        body.put("secretKey", clientSecret);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<NhanhTokenApiResponse> response =
                restTemplate.postForEntity(url, request, NhanhTokenApiResponse.class);

        NhanhTokenApiResponse res = response.getBody();

        if (res == null || res.getCode() != 1 || res.getData() == null) {
            throw new RuntimeException("Nhanh response invalid: " + res);
        }

        NhanhTokenResponse result = new NhanhTokenResponse();
        result.setAccessToken(res.getData().getAccessToken());
        result.setBusinessId(res.getData().getBusinessId());

        return result;
    }
}
