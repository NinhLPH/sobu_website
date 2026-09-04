package com.vn.sodu.voucher.service;

import com.vn.sodu.voucher.GeoScope;
import com.vn.sodu.voucher.Voucher;
import com.vn.sodu.voucher.VoucherScope;
import com.vn.sodu.voucher.VoucherType;
import com.vn.sodu.voucher.dto.VoucherApplyRequestDto;
import com.vn.sodu.voucher.dto.VoucherCartItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VoucherEligibilityService {

    private final CategoryHierarchyService categoryHierarchyService;
    private final VoucherGeoService voucherGeoService;
    private final VoucherDiscountCalculator discountCalculator;

    public String validateGeneralEligibility(Voucher voucher) {
        if (!Boolean.TRUE.equals(voucher.getActive()) || Boolean.TRUE.equals(voucher.getDeleted())) {
            return "mã đã bị vô hiệu hóa hoặc không tồn tại";
        }

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            return "mã chưa đến thời gian sử dụng";
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            return "mã đã hết hạn sử dụng";
        }

        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return "mã đã hết lượt sử dụng";
        }

        return null;
    }

    public String validateVoucherForCheckout(Voucher voucher, VoucherApplyRequestDto request) {
        String generalError = validateGeneralEligibility(voucher);
        if (generalError != null) {
            return generalError;
        }

        BigDecimal subtotal = request.getSubtotal() != null ? request.getSubtotal() : BigDecimal.ZERO;
        List<VoucherCartItemDto> items = request.getItems();

        // 1. Min Order Value check
        if (voucher.getMinOrderValue() != null && voucher.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
            if (voucher.getScope() == VoucherScope.ALL) {
                if (subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                    return "chưa đạt giá trị đơn hàng tối thiểu (" + voucher.getMinOrderValue().toPlainString() + " đ)";
                }
            } else {
                BigDecimal eligibleSubtotal = discountCalculator.calculateEligibleSubtotal(voucher, items);
                if (eligibleSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                    if (eligibleSubtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                        return "chưa đạt giá trị tối thiểu cho nhóm sản phẩm áp dụng (" + voucher.getMinOrderValue().toPlainString() + " đ)";
                    }
                } else if (subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
                    return "chưa đạt giá trị đơn hàng tối thiểu (" + voucher.getMinOrderValue().toPlainString() + " đ)";
                }
            }
        }

        // 2. Scope item matching
        if (items != null && !items.isEmpty()) {
            if (voucher.getScope() == VoucherScope.PRODUCT) {
                BigDecimal eligible = discountCalculator.calculateEligibleSubtotal(voucher, items);
                if (eligible.compareTo(BigDecimal.ZERO) <= 0) {
                    return "không có sản phẩm nào trong giỏ hàng áp dụng được mã này";
                }
            } else if (voucher.getScope() == VoucherScope.CATEGORY) {
                BigDecimal eligible = discountCalculator.calculateEligibleSubtotal(voucher, items);
                if (eligible.compareTo(BigDecimal.ZERO) <= 0) {
                    return "không có sản phẩm nào thuộc danh mục áp dụng mã này";
                }
            }
        }

        // 3. Geo validation (for FreeShip)
        if (voucher.getType() == VoucherType.FREE_SHIP && voucher.getGeoScope() == GeoScope.HANOI_CENTER) {
            boolean eligible = voucherGeoService.isAddressEligible(
                    voucher.getGeoScope(),
                    request.getCustomerCityName(),
                    request.getCustomerWardName(),
                    request.getCustomerCityId()
            );
            if (!eligible) {
                return "mã miễn phí vận chuyển chỉ áp dụng cho 42 phường trung tâm Hà Nội";
            }
        }

        return null;
    }

    public boolean isVoucherApplicableToProduct(Voucher voucher, Long productId, Long categoryId) {
        String generalError = validateGeneralEligibility(voucher);
        if (generalError != null) {
            return false;
        }

        if (voucher.getScope() == VoucherScope.ALL) {
            return true;
        }

        if (voucher.getScope() == VoucherScope.PRODUCT) {
            return voucher.getApplicableProductIds() != null && voucher.getApplicableProductIds().contains(productId);
        }

        if (voucher.getScope() == VoucherScope.CATEGORY) {
            if (categoryId == null || voucher.getApplicableCategoryIds() == null || voucher.getApplicableCategoryIds().isEmpty()) {
                return false;
            }
            Set<Long> expanded = categoryHierarchyService.expandDescendantCategoryIds(voucher.getApplicableCategoryIds());
            return expanded.contains(categoryId);
        }

        return false;
    }
}
