package com.vn.sodu.nhanh.controller;

import com.vn.sodu.nhanh.service.NhanhWebhookService;
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
@RequestMapping("/api/nhanh/webhooks")
@RequiredArgsConstructor
@Tag(name = "Nhanh Webhooks", description = "Public callback endpoint for Nhanh webhook events")
public class NhanhWebhookController {

    private final NhanhWebhookService nhanhWebhookService;

    @PostMapping({"", "/", "/callback"})
    @Operation(
            summary = "Receive Nhanh webhook callback",
            description = "Accepts Nhanh webhook POST callbacks and always returns 200 after recording the payload."
    )
    public ResponseEntity<String> callback(
            @RequestBody(required = false) String body,
            @RequestHeader HttpHeaders headers
    ) {
        nhanhWebhookService.receive(body, headers);
        return ResponseEntity.ok("OK");
    }
}
