package com.vn.sodu.integration;

import com.vn.sodu.global.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/integrations")
@RequiredArgsConstructor
@Tag(name = "Integrations", description = "Public read-only integration status endpoints")
public class IntegrationController {

    private final NhanhEnabled nhanhEnabled;

    @GetMapping("/nhanh")
    @Operation(
            summary = "Get Nhanh integration status",
            description = "Returns whether the Nhanh ERP integration is enabled. When disabled the app runs in local mode."
    )
    public ResponseEntity<ApiResponseDTO<Map<String, Boolean>>> nhanhStatus() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                Map.of("enabled", nhanhEnabled.isEnabled()),
                "Nhanh integration status retrieved"));
    }
}
