package com.vn.sodu.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed configuration for file storage settings.
 * Replaces {@code @Value("${app.storage.*}")} in LocalStorageServiceImpl.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String type = "local";

    private Local local = new Local();

    @Getter
    @Setter
    public static class Local {
        private String dir = "uploads";
    }
}
