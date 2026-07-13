package com.vn.sodu.nhanh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEvent;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLog;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogRepository;
import com.vn.sodu.nhanh.webhook.NhanhWebhookEventLogStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NhanhWebhookServiceTest {

    @Mock
    private NhanhWebhookEventLogRepository repository;

    @Captor
    private ArgumentCaptor<NhanhWebhookEventLog> captor;

    private NhanhProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new NhanhProperties();
        objectMapper = new ObjectMapper();
    }

    @Test
    void persistsSupportedEventWithReceivedStatus() {
        NhanhProperties.Webhooks webhooks = new NhanhProperties.Webhooks();
        webhooks.setVerifyToken("verify-token");
        properties.setWebhooks(webhooks);

        NhanhWebhookService service = new NhanhWebhookService(properties, objectMapper, repository);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "verify-token");

        service.receive("{\"event\":\"webhooksEnabled\",\"businessId\":\"10000\",\"data\":{}}", headers);

        verify(repository).save(captor.capture());
        NhanhWebhookEventLog saved = captor.getValue();
        assertThat(saved.getEventName()).isEqualTo("webhooksEnabled");
        assertThat(saved.getEventType()).isEqualTo("WEBHOOKS_ENABLED");
        assertThat(saved.getBusinessId()).isEqualTo("10000");
        assertThat(saved.getStatus()).isEqualTo(NhanhWebhookEventLogStatus.RECEIVED);
        assertThat(saved.isAuthorizationPresent()).isTrue();
        assertThat(saved.getPayloadHash()).isNotBlank();
    }

    @Test
    void persistsWebhooksEnabledEvent() {
        properties.setWebhooks(new NhanhProperties.Webhooks());

        NhanhWebhookService service = new NhanhWebhookService(properties, objectMapper, repository);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "valid-token");

        service.receive("{\"event\":\"webhooksEnabled\",\"businessId\":\"10000\",\"data\":{}}", headers);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventName()).isEqualTo("webhooksEnabled");
        assertThat(captor.getValue().getStatus()).isEqualTo(NhanhWebhookEventLogStatus.RECEIVED);
    }

    @Test
    void doesNotPersistWhenAuthorizationIsInvalid() {
        NhanhProperties.Webhooks webhooks = new NhanhProperties.Webhooks();
        webhooks.setVerifyToken("expected-token");
        properties.setWebhooks(webhooks);

        NhanhWebhookService service = new NhanhWebhookService(properties, objectMapper, repository);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "wrong-token");

        service.receive("{\"event\":\"webhooksEnabled\",\"businessId\":\"10000\",\"data\":{}}", headers);

        verify(repository, never()).save(any());
    }

    @Test
    void persistsUnsupportedEventAsIgnored() {
        properties.setWebhooks(new NhanhProperties.Webhooks());

        NhanhWebhookService service = new NhanhWebhookService(properties, objectMapper, repository);

        service.receive("{\"event\":\"unknownEvent\",\"businessId\":\"10000\",\"data\":{}}", new HttpHeaders());

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventName()).isEqualTo("unknownEvent");
        assertThat(captor.getValue().getEventType()).isNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(NhanhWebhookEventLogStatus.IGNORED);
    }

    @Test
    void doesNotPersistWhenPayloadIsInvalidJson() {
        NhanhProperties.Webhooks webhooks = new NhanhProperties.Webhooks();
        webhooks.setVerifyToken("verify-token");
        properties.setWebhooks(webhooks);

        NhanhWebhookService service = new NhanhWebhookService(properties, objectMapper, repository);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "verify-token");

        service.receive("not-json", headers);

        verify(repository, never()).save(any());
    }

    @Test
    void doesNotPersistWhenPayloadIsBlank() {
        NhanhProperties.Webhooks webhooks = new NhanhProperties.Webhooks();
        webhooks.setVerifyToken("verify-token");
        properties.setWebhooks(webhooks);

        NhanhWebhookService service = new NhanhWebhookService(properties, objectMapper, repository);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "verify-token");

        service.receive("", headers);

        verify(repository, never()).save(any());
    }

    @Test
    void extractsExternalObjectIdFromDataField() {
        properties.setWebhooks(new NhanhProperties.Webhooks());

        NhanhWebhookService service = new NhanhWebhookService(properties, objectMapper, repository);

        service.receive("{\"event\":\"orderUpdate\",\"businessId\":\"10000\",\"data\":{\"id\":\"12345\"}}", new HttpHeaders());

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getExternalObjectId()).isEqualTo("12345");
    }

    @Test
    void computesPayloadHashCorrectly() throws Exception {
        properties.setWebhooks(new NhanhProperties.Webhooks());

        NhanhWebhookService service = new NhanhWebhookService(properties, objectMapper, repository);
        String rawBody = "{\"event\":\"orderUpdate\",\"businessId\":\"10000\",\"data\":{}}";

        service.receive(rawBody, new HttpHeaders());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawBody.getBytes(StandardCharsets.UTF_8));
        StringBuilder expected = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            expected.append(String.format("%02x", b));
        }

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPayloadHash()).isEqualTo(expected.toString());
    }

    @Test
    void persistsAllConfiguredWebhookEvents() {
        properties.setWebhooks(new NhanhProperties.Webhooks());

        NhanhWebhookService service = new NhanhWebhookService(properties, objectMapper, repository);

        for (NhanhWebhookEvent event : NhanhWebhookEvent.values()) {
            String payload = "{\"event\":\"" + event.eventName() + "\",\"businessId\":10000,\"data\":{}}";

            assertThatCode(() -> service.receive(payload, new HttpHeaders()))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void doesNotThrowWhenAnyExceptionOccurs() {
        // repository.save throws intentionally — service must not propagate exception
        NhanhWebhookService service = new NhanhWebhookService(properties, objectMapper, repository);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "verify-token");

        assertThatCode(() -> service.receive("{\"event\":\"orderUpdate\"}", headers))
                .doesNotThrowAnyException();
    }
}
