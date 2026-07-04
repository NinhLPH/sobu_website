package com.vn.sodu.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private String[] allowedOrigins = {
        "http://localhost:5173", "http://localhost:5174",
        "http://localhost:8081", "http://localhost:3000",
        "https://sobu-jet.vercel.app"
    };

    private long maxAge = 3600;
}
