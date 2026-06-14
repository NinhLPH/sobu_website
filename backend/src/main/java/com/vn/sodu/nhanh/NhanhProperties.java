package com.vn.sodu.nhanh;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration for all Nhanh integration properties.
 * Replaces scattered {@code @Value("${nhanh.*}")} across sync services,
 * NhanhClient, NhanhService, and NhanhController.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "nhanh")
public class NhanhProperties {

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    @NotBlank
    private String redirectUri;

    @NotBlank
    private String baseUrl = "https://pos.open.nhanh.vn/api";

    /** Business ID — required for product/brand/category sync. */
    @NotBlank
    private String businessId;

    private Long depotId;

    private Sync sync = new Sync();
    private Webhooks webhooks = new Webhooks();
    private Accounting accounting = new Accounting();
    private Location location = new Location();

    @Getter
    @Setter
    public static class Sync {
        private String cron = "0 0 * * * *";
        private Recovery recovery = new Recovery();
    }

    @Getter
    @Setter
    public static class Recovery {
        private boolean enabled = true;
        private long initialDelayMs = 120_000L;
        private long fixedDelayMs = 300_000L;
        private int batchSize = 50;
        private int maxRetries = 5;
    }

    @Getter
    @Setter
    public static class Webhooks {
        private String verifyToken;
    }

    @Getter
    @Setter
    public static class Accounting {
        private Long accountId;
    }

    @Getter
    @Setter
    public static class Location {
        private String path = "/v3.0/shipping/location";
        private String version = "v1";
        private long cacheTtlHours = 24;
    }
}
