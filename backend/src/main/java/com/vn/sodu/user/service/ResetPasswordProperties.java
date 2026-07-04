package com.vn.sodu.user.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.reset-password")
public class ResetPasswordProperties {

    private String secretKey;

    private String frontendResetPasswordUrl;

    private long tokenExpirationMs = 900_000;

    private long cooldownMs = 60_000;
}
