package com.vn.sodu.product.category.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.product.category.dto.CategoryDTO;
import com.vn.sodu.product.category.dto.CategoryListItemDTO;
import com.vn.sodu.product.category.dto.CategoryRequest;
import com.vn.sodu.product.category.service.AdminCategoryService;
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
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<CategoryListItemDTO>>> getAllCategories(Authentication authentication) {
        requireStaff(authentication);
        List<CategoryListItemDTO> categories = adminCategoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponseDTO.success(categories, "Categories retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CategoryDTO>> getCategoryById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        requireStaff(authentication);
        CategoryDTO category = adminCategoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(category, "Category retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<CategoryDTO>> createCategory(
            Authentication authentication,
            @RequestBody CategoryRequest request
    ) {
        requireStaff(authentication);
        CategoryDTO category = adminCategoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(category, "Category created successfully", HttpStatus.CREATED.value()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<CategoryDTO>> updateCategory(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody CategoryRequest request
    ) {
        requireStaff(authentication);
        CategoryDTO category = adminCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(category, "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteCategory(
            Authentication authentication,
            @PathVariable Long id
    ) {
        requireStaff(authentication);
        adminCategoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Category deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponseDTO<CategoryDTO>> setCategoryStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody StatusRequest request
    ) {
        requireStaff(authentication);
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Status field is required");
        }
        CategoryDTO category = adminCategoryService.setCategoryStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponseDTO.success(category, "Category status updated successfully"));
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