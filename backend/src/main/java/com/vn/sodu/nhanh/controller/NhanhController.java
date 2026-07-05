package com.vn.sodu.nhanh.controller;

import com.vn.sodu.nhanh.service.NhanhService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/nhanh")
@RequiredArgsConstructor
@Tag(name = "Nhanh Integration", description = "OAuth and connection endpoints for Nhanh POS integration")
public class NhanhController {

    private final NhanhService nhanhService;
    private final com.vn.sodu.nhanh.NhanhProperties nhanhProperties;

    @GetMapping("/login")
    @Operation(
            summary = "Get Nhanh OAuth login URL",
            description = "Returns the Nhanh OAuth authorization URL to redirect the admin to."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OAuth URL generated successfully")
    })
    public ResponseEntity<String> login() {
        String url = "https://nhanh.vn/oauth?" +
                "version=3.0" +
                "&appId=" + nhanhProperties.getClientId() +
                "&returnLink=" + URLEncoder.encode(nhanhProperties.getRedirectUri(), StandardCharsets.UTF_8);

        return ResponseEntity.ok(url);
    }

    @GetMapping("/oauth/callback")
    @Operation(
            summary = "Handle Nhanh OAuth callback",
            description = "Receives the OAuth access code from Nhanh after admin authorization and stores the token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nhanh connection established successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing access code")
    })
    public ResponseEntity<String> callback(@RequestParam("accessCode") String accessCode) {
        nhanhService.handleCallback(accessCode);
        return ResponseEntity.ok("Connected");
    }
}
