package com.vn.sodu.voucher.service;

import com.vn.sodu.voucher.Voucher;
import com.vn.sodu.voucher.VoucherSlot;
import com.vn.sodu.voucher.VoucherType;
import com.vn.sodu.voucher.dto.AppliedVoucherDto;
import com.vn.sodu.voucher.dto.VoucherApplyRequestDto;
import com.vn.sodu.voucher.dto.VoucherApplyResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VoucherSelectionService {

    private final VoucherEligibilityService eligibilityService;
    private final VoucherDiscountCalculator discountCalculator;

    public SelectedVouchersResult evaluateAndSelectVouchers(
            List<Voucher> activeAutoVouchers,
            Voucher manualItemVoucher,
            Voucher manualOrderVoucher,
            Voucher manualShippingVoucher,
            VoucherApplyRequestDto request
    ) {
        BigDecimal subtotal = request.getSubtotal() != null ? request.getSubtotal() : BigDecimal.ZERO;
        BigDecimal shippingFee = request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO;

        // 1. Evaluate ITEM Slot
        Voucher selectedItemVoucher = manualItemVoucher;
        boolean itemIsAuto = false;
        if (selectedItemVoucher == null && Boolean.TRUE.equals(request.getAutoApply())) {
            selectedItemVoucher = findBestItemVoucher(activeAutoVouchers, request);
            if (selectedItemVoucher != null) {
                itemIsAuto = true;
            }
        }

        // 2. Evaluate ORDER Slot
        Voucher selectedOrderVoucher = manualOrderVoucher;
        boolean orderIsAuto = false;
        if (selectedOrderVoucher == null && Boolean.TRUE.equals(request.getAutoApply())) {
            selectedOrderVoucher = findBestOrderVoucher(activeAutoVouchers, request);
            if (selectedOrderVoucher != null) {
                orderIsAuto = true;
            }
        }

        // 3. Evaluate SHIPPING Slot
        Voucher selectedShippingVoucher = manualShippingVoucher;
        boolean shippingIsAuto = false;
        if (selectedShippingVoucher == null && Boolean.TRUE.equals(request.getAutoApply())) {
            selectedShippingVoucher = findBestShippingVoucher(activeAutoVouchers, request);
            if (selectedShippingVoucher != null) {
                shippingIsAuto = true;
            }
        }

        // Calculate discounts
        BigDecimal itemDiscount = BigDecimal.ZERO;
        if (selectedItemVoucher != null) {
            itemDiscount = discountCalculator.calculateItemDiscount(selectedItemVoucher, request.getItems(), subtotal);
        }

        BigDecimal remainingSubtotalAfterItem = subtotal.subtract(itemDiscount).max(BigDecimal.ZERO);

        BigDecimal orderDiscount = BigDecimal.ZERO;
        if (selectedOrderVoucher != null) {
            orderDiscount = discountCalculator.calculateOrderDiscount(selectedOrderVoucher, remainingSubtotalAfterItem);
        }

        BigDecimal shippingDiscount = BigDecimal.ZERO;
        if (selectedShippingVoucher != null) {
            shippingDiscount = discountCalculator.calculateShippingDiscount(selectedShippingVoucher, shippingFee);
        }

        List<AppliedVoucherDto> appliedList = new ArrayList<>();
        if (selectedItemVoucher != null && itemDiscount.compareTo(BigDecimal.ZERO) > 0) {
            appliedList.add(AppliedVoucherDto.builder()
                    .voucherId(selectedItemVoucher.getId())
                    .code(selectedItemVoucher.getCode())
                    .name(selectedItemVoucher.getName())
                    .slot(VoucherSlot.ITEM)
                    .type(selectedItemVoucher.getType())
                    .discountAmount(itemDiscount)
                    .autoApplied(itemIsAuto)
                    .build());
        }

        if (selectedOrderVoucher != null && orderDiscount.compareTo(BigDecimal.ZERO) > 0) {
            appliedList.add(AppliedVoucherDto.builder()
                    .voucherId(selectedOrderVoucher.getId())
                    .code(selectedOrderVoucher.getCode())
                    .name(selectedOrderVoucher.getName())
                    .slot(VoucherSlot.ORDER)
                    .type(selectedOrderVoucher.getType())
                    .discountAmount(orderDiscount)
                    .autoApplied(orderIsAuto)
                    .build());
        }

        if (selectedShippingVoucher != null && shippingDiscount.compareTo(BigDecimal.ZERO) > 0) {
            appliedList.add(AppliedVoucherDto.builder()
                    .voucherId(selectedShippingVoucher.getId())
                    .code(selectedShippingVoucher.getCode())
                    .name(selectedShippingVoucher.getName())
                    .slot(VoucherSlot.SHIPPING)
                    .type(selectedShippingVoucher.getType())
                    .discountAmount(shippingDiscount)
                    .autoApplied(shippingIsAuto)
                    .build());
        }

        return new SelectedVouchersResult(
                selectedItemVoucher,
                selectedOrderVoucher,
                selectedShippingVoucher,
                itemDiscount,
                orderDiscount,
                shippingDiscount,
                appliedList
        );
    }

    private Voucher findBestItemVoucher(List<Voucher> autoVouchers, VoucherApplyRequestDto request) {
        Voucher best = null;
        BigDecimal maxDiscount = BigDecimal.ZERO;

        for (Voucher v : autoVouchers) {
            if (v.getSlot() != VoucherSlot.ITEM || v.getType() == VoucherType.FREE_SHIP) {
                continue;
            }
            if (eligibilityService.validateVoucherForCheckout(v, request) == null) {
                BigDecimal discount = discountCalculator.calculateItemDiscount(v, request.getItems(), request.getSubtotal());
                if (discount.compareTo(maxDiscount) > 0) {
                    maxDiscount = discount;
                    best = v;
                }
            }
        }
        return best;
    }

    private Voucher findBestOrderVoucher(List<Voucher> autoVouchers, VoucherApplyRequestDto request) {
        Voucher best = null;
        BigDecimal maxDiscount = BigDecimal.ZERO;

        for (Voucher v : autoVouchers) {
            if (v.getSlot() != VoucherSlot.ORDER || v.getType() == VoucherType.FREE_SHIP) {
                continue;
            }
            if (eligibilityService.validateVoucherForCheckout(v, request) == null) {
                BigDecimal discount = discountCalculator.calculateOrderDiscount(v, request.getSubtotal());
                if (discount.compareTo(maxDiscount) > 0) {
                    maxDiscount = discount;
                    best = v;
                }
            }
        }
        return best;
    }

    private Voucher findBestShippingVoucher(List<Voucher> autoVouchers, VoucherApplyRequestDto request) {
        Voucher best = null;
        BigDecimal maxDiscount = BigDecimal.ZERO;

        for (Voucher v : autoVouchers) {
            if (v.getSlot() != VoucherSlot.SHIPPING && v.getType() != VoucherType.FREE_SHIP) {
                continue;
            }
            if (eligibilityService.validateVoucherForCheckout(v, request) == null) {
                BigDecimal discount = discountCalculator.calculateShippingDiscount(v, request.getShippingFee());
                if (discount.compareTo(maxDiscount) > 0) {
                    maxDiscount = discount;
                    best = v;
                }
            }
        }
        return best;
    }

    public record SelectedVouchersResult(
            Voucher itemVoucher,
            Voucher orderVoucher,
            Voucher shippingVoucher,
            BigDecimal itemDiscount,
            BigDecimal orderDiscount,
            BigDecimal shippingDiscount,
            List<AppliedVoucherDto> appliedVouchers
    ) {}
}
