package com.vn.sodu.ui.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.ui.Banner;
import com.vn.sodu.ui.dto.BannerDTO;
import com.vn.sodu.ui.dto.WebsiteConfigurationDTO;
import com.vn.sodu.ui.service.BannerService;
import com.vn.sodu.ui.service.WebConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/ui")
@RequiredArgsConstructor
public class PublicUiController {

    private final BannerService bannerService;
    private final WebConfigService webConfigService;

    @GetMapping("/banners")
    public ResponseEntity<ApiResponseDTO<List<BannerDTO>>> getActiveBanners(
            @RequestParam(required = false) Banner.DeviceType deviceType,
            @RequestParam(required = false) String position) {
        List<BannerDTO> banners = bannerService.getActiveBanners(deviceType, position);
        return ResponseEntity.ok(ApiResponseDTO.success(banners, "Active banners retrieved successfully"));
    }

    @GetMapping("/configs")
    public ResponseEntity<ApiResponseDTO<List<WebsiteConfigurationDTO>>> getPublicConfigs() {
        List<WebsiteConfigurationDTO> configs = webConfigService.getPublicConfigs();
        return ResponseEntity.ok(ApiResponseDTO.success(configs, "Public configurations retrieved successfully"));
    }

    @GetMapping("/configs/{key}")
    public ResponseEntity<ApiResponseDTO<WebsiteConfigurationDTO>> getConfigByKey(@PathVariable String key) {
        WebsiteConfigurationDTO config = webConfigService.getConfigByKey(key);
        return ResponseEntity.ok(ApiResponseDTO.success(config, "Configuration retrieved successfully"));
    }
}
