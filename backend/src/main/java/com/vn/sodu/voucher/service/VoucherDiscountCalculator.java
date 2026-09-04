package com.vn.sodu.voucher.service;

import com.vn.sodu.voucher.Voucher;
import com.vn.sodu.voucher.VoucherScope;
import com.vn.sodu.voucher.VoucherType;
import com.vn.sodu.voucher.dto.VoucherCartItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VoucherDiscountCalculator {

    private final CategoryHierarchyService categoryHierarchyService;

    public BigDecimal calculateItemDiscount(Voucher voucher, List<VoucherCartItemDto> items, BigDecimal subtotalFallback) {
        if (items == null || items.isEmpty()) {
            if (voucher.getScope() == VoucherScope.ALL) {
                return calculateDiscountFromSubtotal(voucher, subtotalFallback != null ? subtotalFallback : BigDecimal.ZERO);
            }
            return BigDecimal.ZERO;
        }

        BigDecimal eligibleSubtotal = calculateEligibleSubtotal(voucher, items);
        if (eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return calculateDiscountFromSubtotal(voucher, eligibleSubtotal);
    }

    public BigDecimal calculateEligibleSubtotal(Voucher voucher, List<VoucherCartItemDto> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (voucher.getScope() == VoucherScope.ALL) {
            return items.stream()
                    .map(item -> (item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 1)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        if (voucher.getScope() == VoucherScope.PRODUCT) {
            Set<Long> targetProductIds = voucher.getApplicableProductIds() != null
                    ? voucher.getApplicableProductIds()
                    : Collections.emptySet();
            return items.stream()
                    .filter(item -> item.getProductId() != null && targetProductIds.contains(item.getProductId()))
                    .map(item -> (item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 1)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        if (voucher.getScope() == VoucherScope.CATEGORY) {
            Set<Long> targetCategoryIds = voucher.getApplicableCategoryIds() != null
                    ? voucher.getApplicableCategoryIds()
                    : Collections.emptySet();
            Set<Long> expandedCategoryIds = categoryHierarchyService.expandDescendantCategoryIds(targetCategoryIds);

            return items.stream()
                    .filter(item -> isItemInCategory(item, expandedCategoryIds))
                    .map(item -> (item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 1)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return BigDecimal.ZERO;
    }

    private boolean isItemInCategory(VoucherCartItemDto item, Set<Long> expandedCategoryIds) {
        if (item.getCategoryId() != null && expandedCategoryIds.contains(item.getCategoryId())) {
            return true;
        }
        if (item.getCategoryIds() != null) {
            for (Long catId : item.getCategoryIds()) {
                if (expandedCategoryIds.contains(catId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public BigDecimal calculateOrderDiscount(Voucher voucher, BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return calculateDiscountFromSubtotal(voucher, subtotal);
    }

    public BigDecimal calculateDiscountFromSubtotal(Voucher voucher, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (voucher.getType() == VoucherType.DISCOUNT_PERCENT) {
            BigDecimal percent = voucher.getValue() != null ? voucher.getValue() : BigDecimal.ZERO;
            discount = amount.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(voucher.getMaxDiscountAmount());
            }
        } else if (voucher.getType() == VoucherType.DISCOUNT_AMOUNT) {
            BigDecimal value = voucher.getValue() != null ? voucher.getValue() : BigDecimal.ZERO;
            discount = amount.min(value);
        }
        return discount.max(BigDecimal.ZERO);
    }

    public BigDecimal calculateShippingDiscount(Voucher voucher, BigDecimal shippingFee) {
        if (shippingFee == null || shippingFee.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = shippingFee;
        if (voucher.getValue() != null && voucher.getValue().compareTo(BigDecimal.ZERO) > 0) {
            discount = shippingFee.min(voucher.getValue());
        }

        if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.min(voucher.getMaxDiscountAmount());
        }

        return discount.max(BigDecimal.ZERO);
    }

    /**
     * Calculate the conditional display price from the current retail price. The old
     * price is only a compatibility fallback for callers that do not have a retail
     * price; a legitimate zero retail price must remain zero.
     */
    public ProductDisplayPriceResult calculateProductDisplayPrice(Voucher voucher, BigDecimal oldPrice, BigDecimal retailPrice) {
        BigDecimal basePrice = retailPrice != null ? retailPrice : oldPrice;
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            return new ProductDisplayPriceResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal discount = calculateDiscountFromSubtotal(voucher, basePrice);
        BigDecimal newPriceForDisplay = basePrice.subtract(discount).max(BigDecimal.ZERO);
        return new ProductDisplayPriceResult(discount, newPriceForDisplay);
    }

    public record ProductDisplayPriceResult(BigDecimal estimatedDiscount, BigDecimal effectivePrice) {}
}
