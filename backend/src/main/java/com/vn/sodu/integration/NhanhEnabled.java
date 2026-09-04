package com.vn.sodu.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Convenience gate for the Nhanh integration master switch
 * ({@code integration.nhanh.enabled}).
 */
@Component
@RequiredArgsConstructor
public class NhanhEnabled {

    private final IntegrationProperties properties;

    public boolean isEnabled() {
        return properties.getNhanh().isEnabled();
    }

    /** Throws when the Nhanh integration is disabled (local mode). */
    public void requireEnabled() {
        if (!isEnabled()) {
            throw new NhanhIntegrationDisabledException();
        }
    }
}
