package com.vn.sodu.payment.service;

import com.vn.sodu.payment.PayOSProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PayOSWebhookRegistrarTest {

    @Test
    void realModeConfirmsConfiguredPublicHttpsWebhook() throws Exception {
        PayOSProperties properties = realProperties();
        PayOSWebhookRegistrationClient client = Mockito.mock(PayOSWebhookRegistrationClient.class);
        PayOSWebhookRegistrar registrar = new PayOSWebhookRegistrar(properties, client);

        registrar.run(null);

        verify(client).confirmWebhook("https://api.example.com/api/payos/webhooks/callback");
    }

    @Test
    void mockModeDoesNotCallPayOS() throws Exception {
        PayOSProperties properties = realProperties();
        properties.setGatewayMode("mock");
        PayOSWebhookRegistrationClient client = Mockito.mock(PayOSWebhookRegistrationClient.class);

        new PayOSWebhookRegistrar(properties, client).run(null);

        verifyNoInteractions(client);
    }

    @Test
    void nonHttpsWebhookFailsBeforeCallingPayOS() {
        PayOSProperties properties = realProperties();
        properties.setWebhookUrl("http://api.example.com/api/payos/webhooks/callback");
        PayOSWebhookRegistrationClient client = Mockito.mock(PayOSWebhookRegistrationClient.class);

        assertThatThrownBy(() -> new PayOSWebhookRegistrar(properties, client).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("public HTTPS");
        verifyNoInteractions(client);
    }

    @Test
    void confirmationFailurePropagatesAndFailsStartup() {
        PayOSProperties properties = realProperties();
        PayOSWebhookRegistrationClient client = Mockito.mock(PayOSWebhookRegistrationClient.class);
        doThrow(new IllegalStateException("PayOS rejected webhook"))
                .when(client).confirmWebhook(properties.getWebhookUrl());

        assertThatThrownBy(() -> new PayOSWebhookRegistrar(properties, client).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PayOS rejected webhook");
    }

    private PayOSProperties realProperties() {
        PayOSProperties properties = new PayOSProperties();
        properties.setGatewayMode("real");
        properties.setClientId("client-id");
        properties.setApiKey("api-key");
        properties.setWebhookUrl("https://api.example.com/api/payos/webhooks/callback");
        return properties;
    }
}
