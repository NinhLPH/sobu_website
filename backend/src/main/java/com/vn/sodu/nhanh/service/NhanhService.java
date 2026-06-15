package com.vn.sodu.nhanh.service;

import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.NhanhIntegration;
import com.vn.sodu.nhanh.NhanhIntegrationRepo;
import com.vn.sodu.nhanh.NhanhOAuthConnectedEvent;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.NhanhTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhService {

    private static final long TOKEN_EXPIRY_SAFETY_SECONDS = 60;

    private final NhanhClient nhanhClient;
    private final NhanhIntegrationRepo nhanhIntegrationRepo;
    private final NhanhProperties nhanhProperties;
    private final ApplicationEventPublisher eventPublisher;

    // 1. Generate login URL
    public String buildAuthUrl() {
        return "https://open.nhanh.vn/oauth/authorize?" +
                "client_id=" + nhanhProperties.getClientId() +
                "&redirect_uri=" + nhanhProperties.getRedirectUri() +
                "&response_type=code";
    }

    @Transactional
    public void handleCallback(String accessCode) {

        NhanhTokenResponse response = nhanhClient.getAccessToken(accessCode);

        log.info("Nhanh response: {}", response);

        if (response == null || response.getAccessToken() == null) {
            throw new RuntimeException("Failed to get accessToken from Nhanh");
        }
        Long configuredBusinessId = Long.valueOf(nhanhProperties.getBusinessId());
        if (!configuredBusinessId.equals(response.getBusinessId())) {
            throw new ExternalServiceException(
                    "Nhanh OAuth businessId does not match the configured business");
        }

        // tìm theo businessId (QUAN TRỌNG)
        NhanhIntegration entity = nhanhIntegrationRepo
                .findByBusinessId(response.getBusinessId())
                .orElse(new NhanhIntegration());

        entity.setBusinessId(response.getBusinessId());
        entity.setAppId(nhanhProperties.getClientId());
        entity.setAccessToken(response.getAccessToken());
        entity.setExpiredAt(response.getExpiredAt());

        nhanhIntegrationRepo.save(entity);
        eventPublisher.publishEvent(new NhanhOAuthConnectedEvent(response.getBusinessId()));

        log.info("Saved/Updated Nhanh token OK, businessId={}", response.getBusinessId());
    }

    // Get valid access token from first active integration
    public String getValidAccessToken() {
        NhanhIntegration integration = getIntegration()
                .orElseThrow(() -> new ExternalServiceException("No Nhanh integration found. Please authenticate first."));

        if (integration.getExpiredAt() == null) {
            throw new ExternalServiceException("Nhanh integration token expiry is missing. Please authenticate again.");
        }

        long now = System.currentTimeMillis() / 1000;
        if (integration.getExpiredAt() <= now + TOKEN_EXPIRY_SAFETY_SECONDS) {
            throw new ExternalServiceException("Nhanh integration token has expired. Please authenticate again.");
        }

        return integration.getAccessToken();
    }

    public java.util.Optional<NhanhIntegration> getIntegration() {
        Long businessId = Long.valueOf(nhanhProperties.getBusinessId());
        return nhanhIntegrationRepo.findByBusinessId(businessId).stream().findFirst();
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
