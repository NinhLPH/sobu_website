package com.vn.sodu.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PayOSWebhookRegistrationServiceTest {

    @Test
    void confirmsConfiguredHttpsWebhookAtStartup() throws Exception {
        PayOSProperties properties = configuredProperties();
        PayOSWebhookConfirmationClient client = mock(PayOSWebhookConfirmationClient.class);

        new PayOSWebhookRegistrationService(properties, client).run(null);

        verify(client).confirmWebhook(
                "https://api.sobu.example/api/payos/webhooks/callback",
                "client-id",
                "api-key"
        );
    }

    @Test
    void rejectsNonHttpsWebhookBeforeCallingPayOS() {
        PayOSProperties properties = configuredProperties();
        properties.setWebhookUrl("http://localhost:8081/api/payos/webhooks/callback");
        PayOSWebhookConfirmationClient client = mock(PayOSWebhookConfirmationClient.class);

        assertThatThrownBy(() -> new PayOSWebhookRegistrationService(properties, client).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("public HTTPS URL");
    }

    @Test
    void failsStartupWhenPayOSRejectsWebhook() {
        PayOSProperties properties = configuredProperties();
        PayOSWebhookConfirmationClient client = mock(PayOSWebhookConfirmationClient.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("PayOS rejected webhook"))
                .when(client)
                .confirmWebhook("https://api.sobu.example/api/payos/webhooks/callback", "client-id", "api-key");

        assertThatThrownBy(() -> new PayOSWebhookRegistrationService(properties, client).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to confirm PayOS webhook URL");
    }

    private PayOSProperties configuredProperties() {
        PayOSProperties properties = new PayOSProperties();
        properties.setClientId("client-id");
        properties.setApiKey("api-key");
        properties.setWebhookUrl("https://api.sobu.example/api/payos/webhooks/callback");
        return properties;
    }
}
