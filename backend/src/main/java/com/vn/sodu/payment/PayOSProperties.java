package com.vn.sodu.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payos")
public class PayOSProperties {

    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String returnUrl = "http://localhost:3000/payment/return";
    private String cancelUrl = "http://localhost:3000/payment/cancel";
    private String webhookUrl = "http://localhost:8081/api/payos/webhooks/callback";
    private String gatewayMode = "mock";
}
