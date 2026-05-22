package com.vn.sodu.ui.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.dto.SearchRequest;
import com.vn.sodu.ui.dto.WebsiteConfigurationDTO;
import com.vn.sodu.ui.dto.WebsiteConfigurationRequest;
import com.vn.sodu.ui.service.WebConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configs")
@RequiredArgsConstructor
public class AdminWebConfigController {

    private final WebConfigService webConfigService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<WebsiteConfigurationDTO>> createConfig(@RequestBody WebsiteConfigurationRequest request) {
        WebsiteConfigurationDTO dto = webConfigService.createConfig(request);
        return ResponseEntity.ok(ApiResponseDTO.success(dto, "Configuration created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<WebsiteConfigurationDTO>> updateConfig(@PathVariable Long id, @RequestBody WebsiteConfigurationRequest request) {
        WebsiteConfigurationDTO dto = webConfigService.updateConfig(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(dto, "Configuration updated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<WebsiteConfigurationDTO>> getConfigById(@PathVariable Long id) {
        WebsiteConfigurationDTO dto = webConfigService.getConfigById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(dto, "Configuration retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteConfig(@PathVariable Long id) {
        webConfigService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Configuration deleted successfully"));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponseDTO<PageResponse<WebsiteConfigurationDTO>>> getAllConfigs(@RequestBody SearchRequest request) {
        PageResponse<WebsiteConfigurationDTO> page = webConfigService.getAllConfigs(request);
        return ResponseEntity.ok(ApiResponseDTO.success(page, "Configurations retrieved successfully"));
    }
}
