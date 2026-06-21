package com.vn.sodu.nhanh.service;

import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.nhanh.NhanhIntegration;
import com.vn.sodu.nhanh.NhanhIntegrationRepo;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.NhanhTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // 1. Generate login URL
    public String buildAuthUrl() {
        return "https://open.nhanh.vn/oauth/authorize?" +
                "client_id=" + nhanhProperties.getClientId() +
                "&redirect_uri=" + nhanhProperties.getRedirectUri() +
                "&response_type=code";
    }

    @Transactional
    public void handleCallback(String accessCode) {

        NhanhTokenResponse response;
        try {
            response = nhanhClient.getAccessToken(accessCode);
        } catch (ExternalServiceException ex) {
            if (isInvalidAccessCode(ex.getMessage())) {
                throw new BadRequestException(
                        "Nhanh access code is invalid or expired. Please start the Nhanh login flow again.");
            }
            throw ex;
        }

        log.info("Nhanh response: {}", response);

        if (response == null || response.getAccessToken() == null) {
            throw new ExternalServiceException("Nhanh token response is missing accessToken");
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

        log.info("Saved/Updated Nhanh token OK, businessId={}", response.getBusinessId());
    }

    private boolean isInvalidAccessCode(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("accesscode")
                && (normalized.contains("expired")
                || normalized.contains("invalid")
                || normalized.contains("not found"));
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
