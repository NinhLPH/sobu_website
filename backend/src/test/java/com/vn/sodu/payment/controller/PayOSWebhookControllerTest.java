package com.vn.sodu.payment.controller;

import com.vn.sodu.payment.service.PayOSWebhookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PayOSWebhookControllerTest {

    @Mock
    private PayOSWebhookService payOSWebhookService;

    @Test
    void callbackReturnsOkAndDelegatesPayload() {
        PayOSWebhookController controller = new PayOSWebhookController(payOSWebhookService);
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-payos-signature", "sig-901");
        String payload = "{\"event\":\"payment.paid\",\"success\":true}";

        ResponseEntity<String> response = controller.callback(payload, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(payOSWebhookService).receive(payload, headers);
    }

    @Test
    void callbackMappingAcceptsBasePathAndCallbackPath() throws NoSuchMethodException {
        Method method = PayOSWebhookController.class.getMethod("callback", String.class, HttpHeaders.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(Arrays.asList(mapping.value())).contains("", "/", "/callback");
    }
}
