package com.vn.sodu.nhanh;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed configuration for all Nhanh integration properties.
 * Replaces scattered {@code @Value("${nhanh.*}")} across sync services,
 * NhanhClient, NhanhService, and NhanhController.
 */
@Getter
@Setter
@Validated
@Component
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
    private Shipping shipping = new Shipping();

    @Getter
    @Setter
    public static class Sync {
        private String cron = "0 0 */12 * * *";
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
        private long requestIntervalMs = 250;
        private int rateLimitMaxAttempts = 5;
        private long rateLimitUnlockBufferSeconds = 1;
        private int retryAfterSeconds = 30;
        private long rollingWindowMs = 30_000;
        private int rollingWindowMaxRequests = 120;
        private long interChunkSleepMs = 0;
        private int chunkSize = 5;
        private int maxAttemptsPerRequest = 5;
        private long extendedLockThresholdMs = 300_000;
        private List<Long> retryBackoffSeconds = new ArrayList<>(List.of(1L, 2L, 5L));
    }

    @Getter
    @Setter
    public static class Shipping {
        private String feePath = "/v3.0/shipping/fee";
        private Integer type = 1;
        private Carrier carrier = new Carrier();
        private ShippingDefaults defaults = new ShippingDefaults();
        private Origin origin = new Origin();
    }

    @Getter
    @Setter
    public static class Carrier {
        private Long id;
        private String standardService;
        private String expressService;
        private Long expressCarrierId;
        private Long expressFallbackId;
    }

    @Getter
    @Setter
    public static class ShippingDefaults {
        private java.math.BigDecimal weight = new java.math.BigDecimal("1000");
    }

    @Getter
    @Setter
    public static class Origin {
        private String address;
        private Long cityId;
        private Long districtId;
        private Long wardId;
    }
}
