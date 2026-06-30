package com.vn.sodu.ui.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.dto.SearchRequest;
import com.vn.sodu.ui.dto.StaticPageDTO;
import com.vn.sodu.ui.dto.StaticPageRequest;
import com.vn.sodu.ui.service.StaticPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/static-pages")
public class AdminStaticPageController {

    private final StaticPageService staticPageService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponseDTO<PageResponse<StaticPageDTO>>> searchPages(
            Authentication authentication,
            @RequestBody(required = false) SearchRequest request
    ) {
        requireStaff(authentication);
        PageResponse<StaticPageDTO> page = staticPageService.searchPages(request);
        return ResponseEntity.ok(ApiResponseDTO.success(page, "Static pages retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<StaticPageDTO>> getPageById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        requireStaff(authentication);
        StaticPageDTO page = staticPageService.getPageById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(page, "Static page retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<StaticPageDTO>> createPage(
            Authentication authentication,
            @RequestBody StaticPageRequest request
    ) {
        requireStaff(authentication);
        StaticPageDTO page = staticPageService.createPage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(page, "Static page created successfully", HttpStatus.CREATED.value()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<StaticPageDTO>> updatePage(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody StaticPageRequest request
    ) {
        requireStaff(authentication);
        StaticPageDTO page = staticPageService.updatePage(id, request);
        return ResponseEntity.ok(ApiResponseDTO.success(page, "Static page updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deletePage(
            Authentication authentication,
            @PathVariable Long id
    ) {
        requireStaff(authentication);
        staticPageService.deletePage(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Static page deleted successfully"));
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
}
