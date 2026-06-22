package com.vn.sodu.payment.controller;

import com.vn.sodu.payment.service.PayOSWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payos/webhooks")
@RequiredArgsConstructor
@Tag(name = "PayOS Webhooks", description = "Public callback endpoint for PayOS payment notifications")
public class PayOSWebhookController {

    private final PayOSWebhookService payOSWebhookService;

    @PostMapping({"", "/", "/callback"})
    @Operation(
            summary = "Receive PayOS webhook callback",
            description = "Accepts PayOS payment callbacks, records the raw payload, and safely applies payment confirmation when applicable."
    )
    public ResponseEntity<String> callback(
            @RequestBody(required = false) String body,
            @RequestHeader HttpHeaders headers
    ) {
        payOSWebhookService.receive(body, headers);
        return ResponseEntity.ok("OK");
    }
}
