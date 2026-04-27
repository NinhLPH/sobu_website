package com.vn.sodu.product.brand.controller;

import com.vn.sodu.product.brand.service.BrandSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/brands")
@Tag(name = "Brand Sync", description = "Admin endpoints for syncing brands from the upstream source")
public class BrandSyncController {

    private final BrandSyncService brandSyncService;

    @PostMapping("/sync")
    @Operation(
            summary = "Sync brands",
            description = "Triggers a full brand sync from the upstream Nhanh source into the local database."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<Map<String, String>> syncBrands() {
        brandSyncService.syncBrands();
        return ResponseEntity.ok(Map.of("message", "Sync success"));
    }
}
