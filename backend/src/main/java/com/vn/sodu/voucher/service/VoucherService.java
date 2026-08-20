package com.vn.sodu.voucher.service;

import com.vn.sodu.voucher.*;
import com.vn.sodu.voucher.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherEligibilityService eligibilityService;
    private final VoucherDiscountCalculator discountCalculator;
    private final VoucherSelectionService selectionService;
    private final CategoryHierarchyService categoryHierarchyService;

    @Transactional(readOnly = true)
    public VoucherApplyResponseDto applyVouchers(VoucherApplyRequestDto request) {
        BigDecimal subtotal = request.getSubtotal() != null ? request.getSubtotal() : BigDecimal.ZERO;
        BigDecimal shippingFee = request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO;

        StringBuilder messageBuilder = new StringBuilder();

        Voucher manualItemVoucher = null;
        Voucher manualOrderVoucher = null;
        Voucher manualShippingVoucher = null;

        // Process explicit item voucher code
        if (request.getItemVoucherCode() != null && !request.getItemVoucherCode().isBlank()) {
            String code = request.getItemVoucherCode().trim();
            Optional<Voucher> vOpt = voucherRepository.findByCodeIgnoreCase(code);
            if (vOpt.isEmpty()) {
                messageBuilder.append("Mã giảm giá sản phẩm '").append(code).append("' không tồn tại. ");
            } else {
                Voucher v = vOpt.get();
                String err = eligibilityService.validateVoucherForCheckout(v, request);
                if (err != null) {
                    messageBuilder.append("Mã sản phẩm '").append(code).append("': ").append(err).append(". ");
                } else if (v.getSlot() != VoucherSlot.ITEM) {
                    messageBuilder.append("Mã '").append(code).append("' không phải là mã giảm theo sản phẩm/danh mục. ");
                } else {
                    manualItemVoucher = v;
                }
            }
        }

        // Process explicit order voucher code
        if (request.getOrderVoucherCode() != null && !request.getOrderVoucherCode().isBlank()) {
            String code = request.getOrderVoucherCode().trim();
            Optional<Voucher> vOpt = voucherRepository.findByCodeIgnoreCase(code);
            if (vOpt.isEmpty()) {
                messageBuilder.append("Mã giảm giá đơn hàng '").append(code).append("' không tồn tại. ");
            } else {
                Voucher v = vOpt.get();
                String err = eligibilityService.validateVoucherForCheckout(v, request);
                if (err != null) {
                    messageBuilder.append("Mã đơn hàng '").append(code).append("': ").append(err).append(". ");
                } else if (v.getSlot() != VoucherSlot.ORDER) {
                    messageBuilder.append("Mã '").append(code).append("' không phải là mã giảm toàn đơn hàng. ");
                } else {
                    manualOrderVoucher = v;
                }
            }
        }

        // Process generic discount voucher code (backward compatibility)
        if (request.getDiscountVoucherCode() != null && !request.getDiscountVoucherCode().isBlank()) {
            String code = request.getDiscountVoucherCode().trim();
            Optional<Voucher> vOpt = voucherRepository.findByCodeIgnoreCase(code);
            if (vOpt.isEmpty()) {
                messageBuilder.append("Mã giảm giá '").append(code).append("' không tồn tại. ");
            } else {
                Voucher v = vOpt.get();
                String err = eligibilityService.validateVoucherForCheckout(v, request);
                if (err != null) {
                    messageBuilder.append("Mã '").append(code).append("': ").append(err).append(". ");
                } else if (v.getType() == VoucherType.FREE_SHIP) {
                    messageBuilder.append("Mã '").append(code).append("' là mã miễn phí vận chuyển, vui lòng áp dụng vào mã vận chuyển. ");
                } else {
                    if (v.getSlot() == VoucherSlot.ITEM) {
                        manualItemVoucher = v;
                    } else {
                        manualOrderVoucher = v;
                    }
                }
            }
        }

        // Process explicit shipping voucher code
        if (request.getShippingVoucherCode() != null && !request.getShippingVoucherCode().isBlank()) {
            String code = request.getShippingVoucherCode().trim();
            Optional<Voucher> vOpt = voucherRepository.findByCodeIgnoreCase(code);
            if (vOpt.isEmpty()) {
                messageBuilder.append("Mã miễn phí vận chuyển '").append(code).append("' không tồn tại. ");
            } else {
                Voucher v = vOpt.get();
                String err = eligibilityService.validateVoucherForCheckout(v, request);
                if (err != null) {
                    messageBuilder.append("Mã vận chuyển '").append(code).append("': ").append(err).append(". ");
                } else if (v.getType() != VoucherType.FREE_SHIP && v.getSlot() != VoucherSlot.SHIPPING) {
                    messageBuilder.append("Mã '").append(code).append("' không phải là mã miễn phí vận chuyển. ");
                } else {
                    manualShippingVoucher = v;
                }
            }
        }

        // Get all active auto-apply vouchers
        List<Voucher> activeAutoVouchers = voucherRepository.findByActiveTrue().stream()
                .filter(v -> Boolean.TRUE.equals(v.getAutoApply()))
                .collect(Collectors.toList());

        VoucherSelectionService.SelectedVouchersResult selection = selectionService.evaluateAndSelectVouchers(
                activeAutoVouchers,
                manualItemVoucher,
                manualOrderVoucher,
                manualShippingVoucher,
                request
        );

        BigDecimal itemDiscount = selection.itemDiscount();
        BigDecimal orderDiscount = selection.orderDiscount();
        BigDecimal subtotalDiscount = itemDiscount.add(orderDiscount).min(subtotal);
        BigDecimal shippingDiscount = selection.shippingDiscount().min(shippingFee);
        BigDecimal totalDiscount = subtotalDiscount.add(shippingDiscount);

        BigDecimal finalSubtotal = subtotal.subtract(subtotalDiscount).max(BigDecimal.ZERO);
        BigDecimal finalShippingFee = shippingFee.subtract(shippingDiscount).max(BigDecimal.ZERO);
        BigDecimal finalTotal = finalSubtotal.add(finalShippingFee);

        boolean isValid = messageBuilder.length() == 0;
        String message = isValid ? "Áp dụng mã giảm giá thành công." : messageBuilder.toString().trim();

        String itemCode = selection.itemVoucher() != null ? selection.itemVoucher().getCode() : null;
        String itemName = selection.itemVoucher() != null ? selection.itemVoucher().getName() : null;
        String orderCode = selection.orderVoucher() != null ? selection.orderVoucher().getCode() : null;
        String orderName = selection.orderVoucher() != null ? selection.orderVoucher().getName() : null;
        String shippingCode = selection.shippingVoucher() != null ? selection.shippingVoucher().getCode() : null;
        String shippingName = selection.shippingVoucher() != null ? selection.shippingVoucher().getName() : null;

        String genericDiscountCode = orderCode != null ? orderCode : itemCode;
        String genericDiscountName = orderName != null ? orderName : itemName;

        return VoucherApplyResponseDto.builder()
                .valid(isValid)
                .discountVoucherCode(genericDiscountCode)
                .discountVoucherName(genericDiscountName)
                .itemVoucherCode(itemCode)
                .itemVoucherName(itemName)
                .orderVoucherCode(orderCode)
                .orderVoucherName(orderName)
                .shippingVoucherCode(shippingCode)
                .shippingVoucherName(shippingName)
                .itemDiscount(itemDiscount)
                .orderDiscount(orderDiscount)
                .subtotalDiscount(subtotalDiscount)
                .shippingDiscount(shippingDiscount)
                .totalDiscount(totalDiscount)
                .originalSubtotal(subtotal)
                .originalShippingFee(shippingFee)
                .finalSubtotal(finalSubtotal)
                .finalShippingFee(finalShippingFee)
                .finalTotal(finalTotal)
                .appliedVouchers(selection.appliedVouchers())
                .message(message)
                .build();
    }

    @Transactional
    public void recordVoucherUsage(String itemCode, String orderCode, String shippingCode) {
        Set<String> codes = new HashSet<>();
        if (itemCode != null && !itemCode.isBlank()) codes.add(itemCode.trim());
        if (orderCode != null && !orderCode.isBlank()) codes.add(orderCode.trim());
        if (shippingCode != null && !shippingCode.isBlank()) codes.add(shippingCode.trim());

        for (String code : codes) {
            voucherRepository.findByCodeIgnoreCase(code).ifPresent(v -> {
                int updated = voucherRepository.incrementUsedCountAtomic(v.getId());
                if (updated == 0 && v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) {
                    throw new IllegalStateException("Mã giảm giá '" + code + "' đã hết lượt sử dụng.");
                }
            });
        }
    }

    @Transactional
    public void recordVoucherUsage(String discountCode, String shippingCode) {
        recordVoucherUsage(null, discountCode, shippingCode);
    }

    @Transactional(readOnly = true)
    public List<VoucherSummaryDTO> getApplicableVouchersForProduct(Long productId, Long categoryId, BigDecimal oldPrice, BigDecimal retailPrice) {
        List<Voucher> activeVouchers = voucherRepository.findByActiveTrue();
        List<VoucherSummaryDTO> list = new ArrayList<>();

        for (Voucher v : activeVouchers) {
            if (v.getType() == VoucherType.FREE_SHIP || eligibilityService.isVoucherApplicableToProduct(v, productId, categoryId)) {
                VoucherDiscountCalculator.ProductDisplayPriceResult displayPrice =
                        discountCalculator.calculateProductDisplayPrice(v, oldPrice, retailPrice);

                String badge = null;
                if (v.getType() == VoucherType.FREE_SHIP) {
                    badge = v.getGeoScope() == GeoScope.HANOI_CENTER ? "Freeship 11 quận Hà Nội" : "Miễn phí vận chuyển";
                } else if (displayPrice.estimatedDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    badge = "Giảm " + displayPrice.estimatedDiscount().toPlainString() + "đ";
                }

                list.add(VoucherSummaryDTO.builder()
                        .id(v.getId())
                        .code(v.getCode())
                        .name(v.getName())
                        .type(v.getType())
                        .slot(v.getSlot())
                        .scope(v.getScope())
                        .geoScope(v.getGeoScope())
                        .value(v.getValue())
                        .maxDiscountAmount(v.getMaxDiscountAmount())
                        .minOrderValue(v.getMinOrderValue())
                        .autoApply(v.getAutoApply())
                        .estimatedDiscount(displayPrice.estimatedDiscount())
                        .effectivePrice(displayPrice.effectivePrice())
                        .badgeText(badge)
                        .startDate(v.getStartDate())
                        .endDate(v.getEndDate())
                        .build());
            }
        }
        return list;
    }

    @Transactional(readOnly = true)
    public VoucherSummaryDTO findBestVoucherForProduct(List<VoucherSummaryDTO> vouchers) {
        if (vouchers == null || vouchers.isEmpty()) {
            return null;
        }
        return vouchers.stream()
                .filter(v -> v.getSlot() == VoucherSlot.ITEM && v.getEstimatedDiscount() != null)
                .max(Comparator.comparing(VoucherSummaryDTO::getEstimatedDiscount))
                .orElse(null);
    }

    // CRUD Methods for Admin
    @Transactional(readOnly = true)
    public Page<VoucherDTO> getVouchers(String keyword, Boolean active, VoucherScope scope, VoucherSlot slot, Boolean autoApply, Pageable pageable) {
        Specification<Voucher> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (keyword != null && !keyword.isBlank()) {
                String likePattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), likePattern),
                        cb.like(cb.lower(root.get("name")), likePattern)
                ));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (scope != null) {
                predicates.add(cb.equal(root.get("scope"), scope));
            }
            if (slot != null) {
                predicates.add(cb.equal(root.get("slot"), slot));
            }
            if (autoApply != null) {
                predicates.add(cb.equal(root.get("autoApply"), autoApply));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return voucherRepository.findAll(spec, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<VoucherDTO> getAllVouchers() {
        return voucherRepository.findAll().stream()
                .filter(v -> !Boolean.TRUE.equals(v.getDeleted()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VoucherDTO> getActiveVouchers() {
        LocalDateTime now = LocalDateTime.now();
        return voucherRepository.findByActiveTrue().stream()
                .filter(v -> (v.getStartDate() == null || !now.isBefore(v.getStartDate())))
                .filter(v -> (v.getEndDate() == null || !now.isAfter(v.getEndDate())))
                .filter(v -> (v.getUsageLimit() == null || v.getUsedCount() < v.getUsageLimit()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public VoucherDTO createVoucher(VoucherDTO dto) {
        validateVoucherInvariants(dto);

        if (voucherRepository.existsByCodeIgnoreCase(dto.getCode())) {
            throw new IllegalArgumentException("Mã voucher '" + dto.getCode() + "' đã tồn tại.");
        }

        VoucherSlot slot = resolveSlot(dto);

        Voucher voucher = Voucher.builder()
                .code(dto.getCode().trim().toUpperCase())
                .name(dto.getName())
                .type(dto.getType())
                .slot(slot)
                .scope(dto.getScope() != null ? dto.getScope() : VoucherScope.ALL)
                .geoScope(dto.getType() == VoucherType.FREE_SHIP && dto.getGeoScope() != null ? dto.getGeoScope() : GeoScope.ALL)
                .value(dto.getValue())
                .maxDiscountAmount(dto.getMaxDiscountAmount())
                .minOrderValue(dto.getMinOrderValue())
                .usageLimit(dto.getUsageLimit())
                .usedCount(0)
                .autoApply(Boolean.TRUE.equals(dto.getAutoApply()))
                .applicableProductIds(dto.getApplicableProductIds() != null ? new HashSet<>(dto.getApplicableProductIds()) : new HashSet<>())
                .applicableCategoryIds(dto.getApplicableCategoryIds() != null ? new HashSet<>(dto.getApplicableCategoryIds()) : new HashSet<>())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .deleted(false)
                .build();

        return toDTO(voucherRepository.save(voucher));
    }

    @Transactional
    public VoucherDTO updateVoucher(Long id, VoucherDTO dto) {
        validateVoucherInvariants(dto);

        Voucher voucher = voucherRepository.findById(id)
                .filter(v -> !Boolean.TRUE.equals(v.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy voucher có id: " + id));

        if (!voucher.getCode().equalsIgnoreCase(dto.getCode()) && voucherRepository.existsByCodeIgnoreCase(dto.getCode())) {
            throw new IllegalArgumentException("Mã voucher '" + dto.getCode() + "' đã tồn tại.");
        }

        voucher.setCode(dto.getCode().trim().toUpperCase());
        voucher.setName(dto.getName());
        voucher.setType(dto.getType());
        voucher.setSlot(resolveSlot(dto));
        voucher.setScope(dto.getScope() != null ? dto.getScope() : VoucherScope.ALL);
        voucher.setGeoScope(dto.getType() == VoucherType.FREE_SHIP && dto.getGeoScope() != null ? dto.getGeoScope() : GeoScope.ALL);
        voucher.setValue(dto.getValue());
        voucher.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        voucher.setMinOrderValue(dto.getMinOrderValue());
        voucher.setUsageLimit(dto.getUsageLimit());
        voucher.setAutoApply(Boolean.TRUE.equals(dto.getAutoApply()));
        voucher.setApplicableProductIds(dto.getApplicableProductIds() != null ? new HashSet<>(dto.getApplicableProductIds()) : new HashSet<>());
        voucher.setApplicableCategoryIds(dto.getApplicableCategoryIds() != null ? new HashSet<>(dto.getApplicableCategoryIds()) : new HashSet<>());
        voucher.setStartDate(dto.getStartDate());
        voucher.setEndDate(dto.getEndDate());
        if (dto.getActive() != null) {
            voucher.setActive(dto.getActive());
        }

        return toDTO(voucherRepository.save(voucher));
    }

    @Transactional
    public VoucherDTO toggleVoucherActive(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .filter(v -> !Boolean.TRUE.equals(v.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy voucher có id: " + id));
        voucher.setActive(!Boolean.TRUE.equals(voucher.getActive()));
        return toDTO(voucherRepository.save(voucher));
    }

    @Transactional
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .filter(v -> !Boolean.TRUE.equals(v.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy voucher có id: " + id));
        voucher.setDeleted(true);
        voucher.setActive(false);
        voucherRepository.save(voucher);
    }

    private void validateVoucherInvariants(VoucherDTO dto) {
        if (dto.getCode() == null || dto.getCode().isBlank()) {
            throw new IllegalArgumentException("Mã voucher không được để trống.");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Tên voucher không được để trống.");
        }
        if (dto.getType() == null) {
            throw new IllegalArgumentException("Loại voucher không được để trống.");
        }
        if (dto.getValue() == null || dto.getValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá trị voucher phải lớn hơn hoặc bằng 0.");
        }

        VoucherScope scope = dto.getScope() != null ? dto.getScope() : VoucherScope.ALL;
        if (scope == VoucherScope.PRODUCT) {
            if (dto.getApplicableProductIds() == null || dto.getApplicableProductIds().isEmpty()) {
                throw new IllegalArgumentException("Voucher theo sản phẩm phải chọn ít nhất một sản phẩm.");
            }
            if (dto.getApplicableCategoryIds() != null && !dto.getApplicableCategoryIds().isEmpty()) {
                throw new IllegalArgumentException("Voucher theo sản phẩm không được chứa danh mục áp dụng.");
            }
        } else if (scope == VoucherScope.CATEGORY) {
            if (dto.getApplicableCategoryIds() == null || dto.getApplicableCategoryIds().isEmpty()) {
                throw new IllegalArgumentException("Voucher theo danh mục phải chọn ít nhất một danh mục.");
            }
            if (dto.getApplicableProductIds() != null && !dto.getApplicableProductIds().isEmpty()) {
                throw new IllegalArgumentException("Voucher theo danh mục không được chứa sản phẩm áp dụng.");
            }
        } else if (scope == VoucherScope.ALL) {
            if (dto.getApplicableProductIds() != null && !dto.getApplicableProductIds().isEmpty()) {
                throw new IllegalArgumentException("Voucher toàn đơn hàng không được chứa sản phẩm áp dụng.");
            }
            if (dto.getApplicableCategoryIds() != null && !dto.getApplicableCategoryIds().isEmpty()) {
                throw new IllegalArgumentException("Voucher toàn đơn hàng không được chứa danh mục áp dụng.");
            }
        }

        if (dto.getType() != VoucherType.FREE_SHIP && dto.getGeoScope() != null && dto.getGeoScope() != GeoScope.ALL) {
            throw new IllegalArgumentException("Giới hạn khu vực (GeoScope) chỉ áp dụng cho mã Miễn phí vận chuyển (FREE_SHIP).");
        }
    }

    private VoucherSlot resolveSlot(VoucherDTO dto) {
        if (dto.getSlot() != null) {
            return dto.getSlot();
        }
        if (dto.getType() == VoucherType.FREE_SHIP) {
            return VoucherSlot.SHIPPING;
        }
        VoucherScope scope = dto.getScope() != null ? dto.getScope() : VoucherScope.ALL;
        if (scope == VoucherScope.PRODUCT || scope == VoucherScope.CATEGORY) {
            return VoucherSlot.ITEM;
        }
        return VoucherSlot.ORDER;
    }

    public VoucherDTO toDTO(Voucher voucher) {
        return VoucherDTO.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .type(voucher.getType())
                .slot(voucher.getSlot())
                .scope(voucher.getScope())
                .geoScope(voucher.getGeoScope())
                .value(voucher.getValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderValue(voucher.getMinOrderValue())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .autoApply(voucher.getAutoApply())
                .applicableProductIds(voucher.getApplicableProductIds() != null ? new HashSet<>(voucher.getApplicableProductIds()) : new HashSet<>())
                .applicableCategoryIds(voucher.getApplicableCategoryIds() != null ? new HashSet<>(voucher.getApplicableCategoryIds()) : new HashSet<>())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .active(voucher.getActive())
                .deleted(voucher.getDeleted())
                .createdAt(voucher.getCreatedAt())
                .build();
    }
}
