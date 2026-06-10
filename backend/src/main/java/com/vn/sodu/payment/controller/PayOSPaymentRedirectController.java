package com.vn.sodu.payment.controller;

import com.vn.sodu.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/payos/payments")
@RequiredArgsConstructor
@Tag(name = "PayOS Redirects", description = "Public redirect endpoints for PayOS checkout outcomes")
public class PayOSPaymentRedirectController {

    private final PaymentService paymentService;

    @GetMapping("/cancel")
    @Operation(
            summary = "Handle PayOS checkout cancellation",
            description = "Marks the payment as failed when the shopper cancels checkout and redirects to the configured cancel URL."
    )
    public ResponseEntity<Void> cancel(
            @RequestParam String paymentCode,
            @RequestParam(required = false) String redirect
    ) {
        paymentService.markPaymentFailed(paymentCode, "Payment cancelled by customer");
        URI location = buildRedirectUri(redirect, paymentCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.toString())
                .build();
    }

    private URI buildRedirectUri(String redirect, String paymentCode) {
        String target = (redirect == null || redirect.isBlank()) ? "/" : redirect.trim();
        return UriComponentsBuilder.fromUriString(target)
                .queryParam("paymentCode", paymentCode)
                .queryParam("status", "FAILED")
                .build(true)
                .toUri();
    }
}
