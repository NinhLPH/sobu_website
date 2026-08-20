package com.vn.sodu.voucher.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.voucher.dto.VoucherApplyRequestDto;
import com.vn.sodu.voucher.dto.VoucherApplyResponseDto;
import com.vn.sodu.voucher.dto.VoucherDTO;
import com.vn.sodu.voucher.dto.VoucherSummaryDTO;
import com.vn.sodu.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/vouchers", "/api/vouchers", "/api/public/vouchers", "/api/v1/public/vouchers"})
@RequiredArgsConstructor
public class PublicVoucherController {

    private final VoucherService voucherService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponseDTO<VoucherApplyResponseDto>> applyVouchers(@RequestBody VoucherApplyRequestDto request) {
        VoucherApplyResponseDto result = voucherService.applyVouchers(request);
        return ResponseEntity.ok(ApiResponseDTO.success(result, result.getMessage()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponseDTO<List<VoucherDTO>>> getActiveVouchers() {
        List<VoucherDTO> vouchers = voucherService.getActiveVouchers();
        return ResponseEntity.ok(ApiResponseDTO.success(vouchers, "Lấy danh sách mã giảm giá thành công."));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponseDTO<List<VoucherSummaryDTO>>> getProductVouchers(
            @PathVariable Long productId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal oldPrice,
            @RequestParam(required = false) BigDecimal price
    ) {
        List<VoucherSummaryDTO> vouchers = voucherService.getApplicableVouchersForProduct(productId, categoryId, oldPrice, price);
        return ResponseEntity.ok(ApiResponseDTO.success(vouchers, "Lấy danh sách voucher áp dụng cho sản phẩm thành công."));
    }
}
