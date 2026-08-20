package com.vn.sodu.product.brand.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.product.brand.dto.BrandListItemDTO;
import com.vn.sodu.product.brand.dto.BrandRequest;
import com.vn.sodu.product.brand.service.AdminBrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/brands")
public class AdminBrandController {

    private final AdminBrandService adminBrandService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<BrandListItemDTO>>> getAllBrands(Authentication authentication) {
        requireStaff(authentication);
        List<BrandListItemDTO> brands = adminBrandService.getAllBrands();
        return ResponseEntity.ok(ApiResponseDTO.success(brands, "Brands retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<BrandListItemDTO>> getBrandById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        requireStaff(authentication);
        BrandListItemDTO brand = adminBrandService.getBrandById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(brand, "Brand retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<BrandListItemDTO>> createBrand(
            Authentication authentication,
            @RequestBody BrandRequest request
    ) {
        requireStaff(authentication);
        BrandListItemDTO brand = adminBrandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(brand, "Brand created successfully", HttpStatus.CREATED.value()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<BrandListItemDTO>> updateBrand(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody BrandRequest request
    ) {
        requireStaff(authentication);
        BrandListItemDTO brand = adminBrandService.updateBrand(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(brand, "Brand updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteBrand(
            Authentication authentication,
            @PathVariable Long id
    ) {
        requireStaff(authentication);
        adminBrandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Brand deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponseDTO<BrandListItemDTO>> setBrandStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody StatusRequest request
    ) {
        requireStaff(authentication);
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Status field is required");
        }
        BrandListItemDTO brand = adminBrandService.setBrandStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponseDTO.success(brand, "Brand status updated successfully"));
    }

    private void requireStaff(Authentication authentication) {
        if (!isStaff(authentication)) {
            throw new AccessDeniedException("Staff access is required");
        }
    }

    private boolean isStaff(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }
            String name = authority.getAuthority().toUpperCase(Locale.ROOT);
            if (name.equals("ROLE_ADMIN") || name.equals("ROLE_STAFF")) {
                return true;
            }
        }
        return false;
    }

    public static class StatusRequest {
        private Integer status;

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}