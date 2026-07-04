package com.vn.sodu.user.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "google")
public class GoogleProperties {

    private String clientId;
}
