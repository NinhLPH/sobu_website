package com.vn.sodu.integration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configuration for the local-mode / Nhanh integration switch.
 *
 * When {@code integration.nhanh.enabled} is false (default), every Nhanh
 * scheduler, HTTP client, OAuth route, webhook, health check, sync/recovery
 * job, shipping quote and location refresh is inert and the application runs
 * fully local. Nhanh credentials are then optional and only loaded when the
 * optional {@code nhanh} Spring profile is active.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {

    private Nhanh nhanh = new Nhanh();
    private Local local = new Local();

    @Getter
    @Setter
    public static class Nhanh {
        /** Master switch for the Nhanh integration. Defaults to off. */
        private boolean enabled = false;
        private VietnamMap vietnamMap = new VietnamMap();
    }

    @Getter
    @Setter
    public static class VietnamMap {
        /** Base URL of the public Vietnam administrative-division API used in local mode. */
        private String baseUrl = "https://provinces.open-api.vn/api";
        /** How long a fetched location tree is cached before a refresh is attempted. */
        private long cacheTtlHours = 720L;
    }

    @Getter
    @Setter
    public static class Local {
        private Shipping shipping = new Shipping();
    }

    @Getter
    @Setter
    public static class Shipping {
        /** Flat shipping fee (VND) applied to quotes when Nhanh is disabled. */
        private BigDecimal flatFee = new BigDecimal("30000");
    }
}
