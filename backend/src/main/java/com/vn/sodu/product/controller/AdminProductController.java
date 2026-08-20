package com.vn.sodu.product.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.product.dto.ProductCreateRequest;
import com.vn.sodu.product.dto.ProductDetailDTO;
import com.vn.sodu.product.dto.ProductFilterRequest;
import com.vn.sodu.product.dto.ProductListItemDTO;
import com.vn.sodu.product.dto.ProductUpdateRequest;
import com.vn.sodu.product.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponse<ProductListItemDTO>>> getAllProducts(
            Authentication authentication,
            @ModelAttribute ProductFilterRequest request
    ) {
        requireStaff(authentication);
        PageResponse<ProductListItemDTO> page = adminProductService.getAllProducts(request);
        return ResponseEntity.ok(ApiResponseDTO.success(page, "Products retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ProductDetailDTO>> getProductById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        requireStaff(authentication);
        ProductDetailDTO product = adminProductService.getProductDetailById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(product, "Product retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<ProductDetailDTO>> createProduct(
            Authentication authentication,
            @RequestBody ProductCreateRequest request
    ) {
        requireStaff(authentication);
        ProductDetailDTO product = adminProductService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(product, "Product created successfully", HttpStatus.CREATED.value()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ProductDetailDTO>> updateProduct(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ProductUpdateRequest request
    ) {
        requireStaff(authentication);
        ProductDetailDTO product = adminProductService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(product, "Product updated successfully"));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponseDTO<ProductDetailDTO>> setActive(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ActiveRequest request
    ) {
        requireStaff(authentication);
        if (request == null || request.getActive() == null) {
            throw new IllegalArgumentException("Active field is required");
        }
        ProductDetailDTO product = adminProductService.setActive(id, request.getActive(), request.getReason());
        return ResponseEntity.ok(ApiResponseDTO.success(product, request.getActive() ? "Product activated" : "Product deactivated"));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponseDTO<ProductDetailDTO>> archiveProduct(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ArchiveRequest request
    ) {
        requireStaff(authentication);
        String reason = request != null ? request.getReason() : null;
        ProductDetailDTO product = adminProductService.archiveProduct(id, reason);
        return ResponseEntity.ok(ApiResponseDTO.success(product, "Product archived successfully"));
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

    public static class ActiveRequest {
        private Boolean active;
        private String reason;

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ArchiveRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}