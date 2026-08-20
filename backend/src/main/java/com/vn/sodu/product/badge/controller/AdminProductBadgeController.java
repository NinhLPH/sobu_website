package com.vn.sodu.product.badge.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.product.badge.dto.ProductBadgeDTO;
import com.vn.sodu.product.badge.dto.ProductBadgeRequest;
import com.vn.sodu.product.badge.service.AdminProductBadgeService;
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
@RequestMapping("/api/admin/badges")
public class AdminProductBadgeController {

    private final AdminProductBadgeService adminProductBadgeService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ProductBadgeDTO>>> getAllBadges(Authentication authentication) {
        requireStaff(authentication);
        List<ProductBadgeDTO> badges = adminProductBadgeService.getAllBadges();
        return ResponseEntity.ok(ApiResponseDTO.success(badges, "Badges retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ProductBadgeDTO>> getBadgeById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        requireStaff(authentication);
        ProductBadgeDTO badge = adminProductBadgeService.getBadgeById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(badge, "Badge retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<ProductBadgeDTO>> createBadge(
            Authentication authentication,
            @RequestBody ProductBadgeRequest request
    ) {
        requireStaff(authentication);
        ProductBadgeDTO badge = adminProductBadgeService.createBadge(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(badge, "Badge created successfully", HttpStatus.CREATED.value()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ProductBadgeDTO>> updateBadge(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ProductBadgeRequest request
    ) {
        requireStaff(authentication);
        ProductBadgeDTO badge = adminProductBadgeService.updateBadge(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(badge, "Badge updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteBadge(
            Authentication authentication,
            @PathVariable Long id
    ) {
        requireStaff(authentication);
        adminProductBadgeService.deleteBadge(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Badge deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponseDTO<ProductBadgeDTO>> setBadgeStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody StatusRequest request
    ) {
        requireStaff(authentication);
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Status field is required");
        }
        ProductBadgeDTO badge = adminProductBadgeService.setBadgeStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponseDTO.success(badge, "Badge status updated successfully"));
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