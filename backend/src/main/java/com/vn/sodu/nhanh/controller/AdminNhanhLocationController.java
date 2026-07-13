package com.vn.sodu.nhanh.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.nhanh.service.NhanhLocationSyncCoordinator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/nhanh/locations")
@Tag(name = "Admin Nhanh Locations", description = "Admin endpoints for Nhanh location synchronization")
public class AdminNhanhLocationController {

    private final NhanhLocationSyncCoordinator locationSyncCoordinator;

    @PostMapping("/sync")
    @Operation(
            summary = "Trigger Nhanh location sync",
            description = "Queues an asynchronous refresh of the Nhanh city, district, and ward snapshot."
    )
    public ResponseEntity<ApiResponseDTO<Void>> triggerSync(Authentication authentication) {
        requireStaff(authentication);
        locationSyncCoordinator.triggerManualSync();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponseDTO.success(
                null,
                "Nhanh location sync has been queued",
                HttpStatus.ACCEPTED.value()
        ));
    }

    private void requireStaff(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Staff access is required");
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority != null && ("ROLE_ADMIN".equals(authority.getAuthority())
                    || "ROLE_STAFF".equals(authority.getAuthority()))) {
                return;
            }
        }
        throw new AccessDeniedException("Staff access is required");
    }
}
