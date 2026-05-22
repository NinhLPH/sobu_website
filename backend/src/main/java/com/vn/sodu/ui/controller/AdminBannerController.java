package com.vn.sodu.ui.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.dto.SearchRequest;
import com.vn.sodu.ui.dto.BannerDTO;
import com.vn.sodu.ui.dto.CreateBannerRequest;
import com.vn.sodu.ui.dto.UpdateBannerRequest;
import com.vn.sodu.ui.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerService bannerService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<BannerDTO>> createBanner(
            @RequestPart("banner") CreateBannerRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        BannerDTO dto = bannerService.createBanner(request, image);
        return ResponseEntity.ok(ApiResponseDTO.success(dto, "Banner created successfully"));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<BannerDTO>> updateBanner(
            @PathVariable Long id, 
            @RequestPart("banner") UpdateBannerRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        BannerDTO dto = bannerService.updateBanner(id, request, image);
        return ResponseEntity.ok(ApiResponseDTO.success(dto, "Banner updated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<BannerDTO>> getBannerById(@PathVariable Long id) {
        BannerDTO dto = bannerService.getBannerById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(dto, "Banner retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Banner deleted successfully"));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponseDTO<PageResponse<BannerDTO>>> getAllBanners(@RequestBody SearchRequest request) {
        PageResponse<BannerDTO> page = bannerService.getAllBanners(request);
        return ResponseEntity.ok(ApiResponseDTO.success(page, "Banners retrieved successfully"));
    }
}
