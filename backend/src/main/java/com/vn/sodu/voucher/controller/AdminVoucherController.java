package com.vn.sodu.voucher.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.voucher.VoucherScope;
import com.vn.sodu.voucher.VoucherSlot;
import com.vn.sodu.voucher.dto.VoucherDTO;
import com.vn.sodu.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
public class AdminVoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Page<VoucherDTO>>> getVouchers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) VoucherScope scope,
            @RequestParam(required = false) VoucherSlot slot,
            @RequestParam(required = false) Boolean autoApply,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<VoucherDTO> vouchers = voucherService.getVouchers(keyword, active, scope, slot, autoApply, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(vouchers, "Lấy danh sách mã giảm giá thành công."));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<List<VoucherDTO>>> getAllVouchers() {
        List<VoucherDTO> vouchers = voucherService.getAllVouchers();
        return ResponseEntity.ok(ApiResponseDTO.success(vouchers, "Lấy toàn bộ mã giảm giá thành công."));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<VoucherDTO>> createVoucher(@RequestBody VoucherDTO dto) {
        VoucherDTO created = voucherService.createVoucher(dto);
        return ResponseEntity.ok(ApiResponseDTO.success(created, "Tạo mã giảm giá thành công."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<VoucherDTO>> updateVoucher(@PathVariable Long id, @RequestBody VoucherDTO dto) {
        VoucherDTO updated = voucherService.updateVoucher(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.success(updated, "Cập nhật mã giảm giá thành công."));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponseDTO<VoucherDTO>> toggleVoucherActive(@PathVariable Long id) {
        VoucherDTO updated = voucherService.toggleVoucherActive(id);
        return ResponseEntity.ok(ApiResponseDTO.success(updated, "Thay đổi trạng thái mã thành công."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Xóa mã giảm giá thành công."));
    }
}
