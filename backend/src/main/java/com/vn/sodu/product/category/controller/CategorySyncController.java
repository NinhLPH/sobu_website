package com.vn.sodu.product.category.controller;

import com.vn.sodu.product.category.service.CategorySyncService;
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
@RequestMapping("/api/admin/categories")
@Tag(name = "Category Sync", description = "Admin endpoints for syncing categories from the upstream source")
public class CategorySyncController {

    private final CategorySyncService categorySyncService;

    @PostMapping("/sync")
    @Operation(
            summary = "Sync categories",
            description = "Triggers a full category sync from the upstream Nhanh source into the local database."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class)))
    })
    public ResponseEntity<Map<String, String>> syncCategories() {
        categorySyncService.syncCategories();
        return ResponseEntity.ok(Map.of("message", "Sync success"));
    }
}
