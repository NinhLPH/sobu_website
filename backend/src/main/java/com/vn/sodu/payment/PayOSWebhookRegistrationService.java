package com.vn.sodu.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payos.gateway-mode", havingValue = "real")
public class PayOSWebhookRegistrationService implements ApplicationRunner {

    private final PayOSProperties properties;
    private final PayOSWebhookConfirmationClient confirmationClient;

    @Override
    public void run(ApplicationArguments args) {
        String webhookUrl = properties.getWebhookUrl();
        validateConfiguration(webhookUrl);
        try {
            confirmationClient.confirmWebhook(webhookUrl, properties.getClientId(), properties.getApiKey());
            log.info("Confirmed PayOS webhook URL: {}", webhookUrl);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Unable to confirm PayOS webhook URL: " + webhookUrl, ex);
        }
    }

    private void validateConfiguration(String webhookUrl) {
        if (isBlank(properties.getClientId()) || isBlank(properties.getApiKey())) {
            throw new IllegalStateException("PayOS clientId and apiKey are required when payos.gateway-mode=real");
        }
        if (isBlank(webhookUrl)) {
            throw new IllegalStateException("PayOS webhookUrl is required when payos.gateway-mode=real");
        }
        try {
            URI uri = URI.create(webhookUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || isBlank(uri.getHost())) {
                throw new IllegalStateException("PayOS webhookUrl must be a public HTTPS URL");
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("PayOS webhookUrl must be a valid public HTTPS URL", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
