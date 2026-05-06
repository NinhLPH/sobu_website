package com.vn.sodu.nhanh.health;

import com.vn.sodu.nhanh.NhanhIntegration;
import com.vn.sodu.nhanh.service.NhanhService;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Endpoint(id = "nhanh-health")
public class NhanhHealth {

    private final NhanhService nhanhService;

    public NhanhHealth(NhanhService nhanhService) {
        this.nhanhService = nhanhService;
    }

    @ReadOperation
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("checkedAt", Instant.now().toString());

        var integrationOpt = nhanhService.getIntegration();
        if (integrationOpt.isEmpty()) {
            response.put("status", "DOWN");
            response.put("message", "No Nhanh integration configured");
            response.put("tokenPresent", false);
            return response;
        }

        NhanhIntegration integration = integrationOpt.get();
        Long expiresAt = integration.getExpiredAt();
        Long normalizedExpiresAt = normalizeEpochSeconds(expiresAt);
        long now = Instant.now().getEpochSecond();
        boolean tokenPresent = integration.getAccessToken() != null && !integration.getAccessToken().isBlank();
        boolean expired = !tokenPresent || normalizedExpiresAt == null || normalizedExpiresAt <= now;

        response.put("status", expired ? "DOWN" : "UP");
        response.put("businessId", integration.getBusinessId());
        response.put("tokenPresent", tokenPresent);
        response.put("expiresAt", expiresAt);
        response.put("expiresAtEpochSeconds", normalizedExpiresAt);
        response.put("expiresInSeconds", normalizedExpiresAt == null ? null : normalizedExpiresAt - now);
        response.put("expired", expired);
        response.put("message", expired ? "Nhanh token is expired or missing" : "Nhanh token is valid");
        return response;
    }

    private Long normalizeEpochSeconds(Long timestamp) {
        if (timestamp == null) {
            return null;
        }

        if (Math.abs(timestamp) >= 1_000_000_000_000L) {
            return timestamp / 1000L;
        }

        return timestamp;
    }
}
