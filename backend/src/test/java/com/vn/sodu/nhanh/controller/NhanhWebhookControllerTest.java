package com.vn.sodu.nhanh.controller;

import com.vn.sodu.nhanh.service.NhanhWebhookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NhanhWebhookControllerTest {

    @Mock
    private NhanhWebhookService nhanhWebhookService;

    @Test
    void callbackReturnsOkAndDelegatesPayload() {
        NhanhWebhookController controller = new NhanhWebhookController(nhanhWebhookService);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "verify-token");
        String payload = "{\"event\":\"webhooksEnabled\"}";

        ResponseEntity<String> response = controller.callback(payload, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(nhanhWebhookService).receive(payload, headers);
    }

    @Test
    void callbackMappingAcceptsBasePathAndCallbackPath() throws NoSuchMethodException {
        Method method = NhanhWebhookController.class.getMethod("callback", String.class, HttpHeaders.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(Arrays.asList(mapping.value())).contains("", "/", "/callback");
    }
}
