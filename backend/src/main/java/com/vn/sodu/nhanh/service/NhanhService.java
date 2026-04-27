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

        // tìm theo businessId (QUAN TRỌNG)
        NhanhIntegration entity = nhanhIntegrationRepo
                .findByBusinessId(response.getBusinessId())
                .orElse(new NhanhIntegration());

        entity.setBusinessId(response.getBusinessId());
        entity.setAppId(clientId);
        entity.setAccessToken(response.getAccessToken());
        entity.setExpiredAt(response.getExpiredAt());

        nhanhIntegrationRepo.save(entity);

        log.info("Saved/Updated Nhanh token OK, businessId={}", response.getBusinessId());
    }

    // Get valid access token from first active integration
    public String getValidAccessToken() {
        return getIntegration()
                .map(NhanhIntegration::getAccessToken)
                .orElseThrow(() -> new RuntimeException("No Nhanh integration found. Please authenticate first."));
    }

    public java.util.Optional<NhanhIntegration> getIntegration() {
        return nhanhIntegrationRepo.findAll().stream().findFirst();
    }

    @Transactional
    public void updateLastSyncTime(Long time) {
        getIntegration().ifPresent(entity -> {
            entity.setLastProductSyncTime(time);
            nhanhIntegrationRepo.save(entity);
            log.info("Updated lastSyncTime={} for businessId={}", time, entity.getBusinessId());
        });
    }
}