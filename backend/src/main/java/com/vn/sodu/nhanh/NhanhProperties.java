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

    private Sync sync = new Sync();

    @Getter
    @Setter
    public static class Sync {
        private String cron = "0 0 * * * *";
    }
}
