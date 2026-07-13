package com.vn.sodu.payment.service;

import com.vn.sodu.payment.PayOSProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;

/**
 * Fails startup in real mode if PayOS cannot validate the callback endpoint.
 * This prevents accepting checkout creation while payment confirmations are
 * guaranteed to be lost.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payos.gateway-mode", havingValue = "real")
public class PayOSWebhookRegistrar implements ApplicationRunner {

    private final PayOSProperties payOSProperties;
    private final PayOSWebhookRegistrationClient registrationClient;

    @Override
    public void run(ApplicationArguments args) {
        if (!"real".equalsIgnoreCase(payOSProperties.getGatewayMode())) {
            return;
        }

        String webhookUrl = validatePublicHttpsUrl(payOSProperties.getWebhookUrl());
        registrationClient.confirmWebhook(webhookUrl);
        log.info("PayOS webhook confirmed successfully: webhookUrl={}", webhookUrl);
    }

    private String validatePublicHttpsUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("PAYOS_WEBHOOK_URL is required when PAYOS_GATEWAY_MODE=real");
        }

        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("PAYOS_WEBHOOK_URL must be a valid absolute HTTPS URL", ex);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalStateException("PAYOS_WEBHOOK_URL must be a public HTTPS URL");
        }
        if (isLocalOrPrivateHost(uri.getHost())) {
            throw new IllegalStateException("PAYOS_WEBHOOK_URL must not point to localhost or a private network");
        }
        return uri.toString();
    }

    private boolean isLocalOrPrivateHost(String host) {
        String normalized = host.trim().toLowerCase();
        if (normalized.equals("localhost") || normalized.endsWith(".localhost")) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(normalized);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress();
        } catch (Exception ignored) {
            // A public DNS host is resolved and verified by PayOS itself during confirmation.
            return false;
        }
    }
}
