package com.vn.sodu.inventory.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.inventory.InventoryAdjustmentType;
import com.vn.sodu.inventory.InventoryService;
import com.vn.sodu.inventory.dto.InventoryAdjustmentDto;
import com.vn.sodu.inventory.dto.InventoryAdjustmentRequest;
import com.vn.sodu.inventory.dto.InventoryBalanceDto;
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
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/{productId}/opening")
    public ResponseEntity<ApiResponseDTO<InventoryAdjustmentDto>> setOpeningStock(
            Authentication authentication,
            @PathVariable Long productId,
            @RequestBody OpeningStockRequest request
    ) {
        requireStaff(authentication);
        if (request == null) {
            throw new IllegalArgumentException("Opening stock payload is required");
        }
        InventoryAdjustmentDto dto = inventoryService.setOpeningStock(productId, request.getQuantity(), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(dto, "Opening stock set", HttpStatus.CREATED.value()));
    }

    @PostMapping("/{productId}/adjustments")
    public ResponseEntity<ApiResponseDTO<InventoryAdjustmentDto>> adjust(
            Authentication authentication,
            @PathVariable Long productId,
            @RequestBody InventoryAdjustmentRequest request
    ) {
        requireStaff(authentication);
        if (request == null) {
            throw new IllegalArgumentException("Inventory adjustment payload is required");
        }
        InventoryAdjustmentType type = request.getType();
        InventoryAdjustmentDto dto = inventoryService.adjust(productId, type, request.getQuantity(), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(dto, "Inventory adjusted", HttpStatus.CREATED.value()));
    }

    @GetMapping("/{productId}/balance")
    public ResponseEntity<ApiResponseDTO<InventoryBalanceDto>> getBalance(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        requireStaff(authentication);
        InventoryBalanceDto dto = inventoryService.getBalance(productId);
        return ResponseEntity.ok(ApiResponseDTO.success(dto, "Inventory balance retrieved"));
    }

    @GetMapping("/{productId}/ledger")
    public ResponseEntity<ApiResponseDTO<List<InventoryAdjustmentDto>>> getLedger(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        requireStaff(authentication);
        List<InventoryAdjustmentDto> ledger = inventoryService.getLedger(productId);
        return ResponseEntity.ok(ApiResponseDTO.success(ledger, "Inventory ledger retrieved"));
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

    public static class OpeningStockRequest {
        private Double quantity;
        private String note;

        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}