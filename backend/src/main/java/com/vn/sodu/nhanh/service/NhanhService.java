package com.vn.sodu.nhanh.service;

import com.vn.sodu.nhanh.NhanhIntegration;
import com.vn.sodu.nhanh.NhanhIntegrationRepo;
import com.vn.sodu.nhanh.NhanhTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhService {

    private final NhanhClient nhanhClient;
    private final NhanhIntegrationRepo nhanhIntegrationRepo;

    @Value("${nhanh.client-id}")
    private String clientId;

    @Value("${nhanh.client-secret}")
    private String clientSecret;

    @Value("${nhanh.redirect-uri}")
    private String redirectUri;

    // 1. Generate login URL
    public String buildAuthUrl() {
        return "https://open.nhanh.vn/oauth/authorize?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code";
    }

    @Transactional
    public void handleCallback(String accessCode) {

        NhanhTokenResponse response = nhanhClient.getAccessToken(accessCode);

        log.info("Nhanh response: {}", response);

        if (response == null || response.getAccessToken() == null) {
            throw new RuntimeException("Failed to get accessToken from Nhanh");
        }

        NhanhIntegration entity = new NhanhIntegration();
        entity.setAccessToken(response.getAccessToken());
        entity.setShopId(String.valueOf(response.getBusinessId()));

        nhanhIntegrationRepo.save(entity);

        log.info("Saved Nhanh token OK");
    }
}
