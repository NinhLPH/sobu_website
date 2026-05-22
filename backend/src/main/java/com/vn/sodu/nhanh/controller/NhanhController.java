package com.vn.sodu.nhanh.controller;

import com.vn.sodu.nhanh.service.NhanhService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/nhanh")
@RequiredArgsConstructor
@Tag(name = "Nhanh Integration", description = "OAuth and connection endpoints for Nhanh integration")
public class NhanhController {

    private final NhanhService nhanhService;
    private final com.vn.sodu.nhanh.NhanhProperties nhanhProperties;

    @GetMapping("/oauth/callback")
    public ResponseEntity<String> callback(@RequestParam("accessCode") String accessCode) {
        nhanhService.handleCallback(accessCode);
        return ResponseEntity.ok("Connected");
    }
    @GetMapping("/login")
    public ResponseEntity<String> login() {
        String url = "https://nhanh.vn/oauth?" +
                "version=3.0" +
                "&appId=" + nhanhProperties.getClientId() +
                "&returnLink=" + URLEncoder.encode(nhanhProperties.getRedirectUri(), StandardCharsets.UTF_8);

        return ResponseEntity.ok(url);
    }
}
