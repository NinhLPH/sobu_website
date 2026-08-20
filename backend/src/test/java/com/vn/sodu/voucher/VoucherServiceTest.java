package com.vn.sodu.voucher;

import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.voucher.dto.*;
import com.vn.sodu.voucher.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private CategoryRepo categoryRepo;

    private VoucherService voucherService;
    private CategoryHierarchyService categoryHierarchyService;
    private VoucherGeoService voucherGeoService;
    private VoucherDiscountCalculator discountCalculator;
    private VoucherEligibilityService eligibilityService;
    private VoucherSelectionService selectionService;

    private Voucher percentOrderVoucher;
    private Voucher freeShipHanoiVoucher;
    private Voucher skincareCategoryVoucher;
    private Voucher productSpecificVoucher;

    @BeforeEach
    void setUp() {
        categoryHierarchyService = new CategoryHierarchyService(categoryRepo);
        voucherGeoService = new VoucherGeoService();
        discountCalculator = new VoucherDiscountCalculator(categoryHierarchyService);
        eligibilityService = new VoucherEligibilityService(categoryHierarchyService, voucherGeoService, discountCalculator);
        selectionService = new VoucherSelectionService(eligibilityService, discountCalculator);

        voucherService = new VoucherService(
                voucherRepository,
                eligibilityService,
                discountCalculator,
                selectionService,
                categoryHierarchyService
        );

        // Setup Category Hierarchy: Root Skincare (1) -> Serum (2)
        Category rootCat = Category.builder().id(1L).name("Skincare").build();
        Category childCat = Category.builder().id(2L).parentId(1L).name("Serum").build();
        Category otherCat = Category.builder().id(3L).name("Fashion").build();
        lenient().when(categoryRepo.findAll()).thenReturn(List.of(rootCat, childCat, otherCat));

        percentOrderVoucher = Voucher.builder()
                .id(1L)
                .code("SOBUAUTO5")
                .name("Tự động giảm 5% toàn đơn từ 200k")
                .type(VoucherType.DISCOUNT_PERCENT)
                .slot(VoucherSlot.ORDER)
                .scope(VoucherScope.ALL)
                .value(new BigDecimal("5.00"))
                .maxDiscountAmount(new BigDecimal("50000.00"))
                .minOrderValue(new BigDecimal("200000.00"))
                .usageLimit(100)
                .usedCount(5)
                .autoApply(true)
                .active(true)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .build();

        freeShipHanoiVoucher = Voucher.builder()
                .id(2L)
                .code("HANOIFREE")
                .name("Miễn phí vận chuyển 11 quận nội thành Hà Nội")
                .type(VoucherType.FREE_SHIP)
                .slot(VoucherSlot.SHIPPING)
                .scope(VoucherScope.ALL)
                .geoScope(GeoScope.HANOI_CENTER)
                .value(new BigDecimal("30000.00"))
                .maxDiscountAmount(new BigDecimal("30000.00"))
                .minOrderValue(new BigDecimal("100000.00"))
                .usageLimit(50)
                .usedCount(0)
                .autoApply(true)
                .active(true)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .build();

        skincareCategoryVoucher = Voucher.builder()
                .id(3L)
                .code("SKINCARE10")
                .name("Giảm 10% danh mục Skincare")
                .type(VoucherType.DISCOUNT_PERCENT)
                .slot(VoucherSlot.ITEM)
                .scope(VoucherScope.CATEGORY)
                .applicableCategoryIds(Set.of(1L))
                .value(new BigDecimal("10.00"))
                .maxDiscountAmount(new BigDecimal("50000.00"))
                .minOrderValue(new BigDecimal("150000.00"))
                .active(true)
                .autoApply(false)
                .build();

        productSpecificVoucher = Voucher.builder()
                .id(4L)
                .code("VIPPROD15")
                .name("Giảm 15% sản phẩm VIP")
                .type(VoucherType.DISCOUNT_PERCENT)
                .slot(VoucherSlot.ITEM)
                .scope(VoucherScope.PRODUCT)
                .applicableProductIds(Set.of(101L))
                .value(new BigDecimal("15.00"))
                .maxDiscountAmount(new BigDecimal("60000.00"))
                .active(true)
                .autoApply(false)
                .build();
    }

    @Test
    @DisplayName("Product Voucher Display Calculation uses oldPrice as base for discount formula: newPrice = oldPrice - (oldPrice * voucher%)")
    void testProductVoucherDisplayCalculation_UsesOldPrice() {
        when(voucherRepository.findByActiveTrue()).thenReturn(List.of(productSpecificVoucher));

        BigDecimal oldPrice = new BigDecimal("400000.00");
        BigDecimal retailPrice = new BigDecimal("350000.00");

        List<VoucherSummaryDTO> list = voucherService.getApplicableVouchersForProduct(101L, 2L, oldPrice, retailPrice);

        assertThat(list).hasSize(1);
        VoucherSummaryDTO summary = list.get(0);
        // 15% of 400,000 = 60,000
        assertThat(summary.getEstimatedDiscount()).isEqualByComparingTo("60000.00");
        // Effective price = 400,000 - 60,000 = 340,000
        assertThat(summary.getEffectivePrice()).isEqualByComparingTo("340000.00");
    }

    @Test
    @DisplayName("Order Voucher enforces minOrderValue threshold")
    void testOrderVoucher_RequiresMinimumOrderAmount() {
        when(voucherRepository.findByCodeIgnoreCase("SOBUAUTO5")).thenReturn(Optional.of(percentOrderVoucher));

        // Subtotal below 200k threshold
        VoucherApplyRequestDto request = VoucherApplyRequestDto.builder()
                .orderVoucherCode("SOBUAUTO5")
                .subtotal(new BigDecimal("150000.00"))
                .shippingFee(new BigDecimal("30000.00"))
                .build();

        VoucherApplyResponseDto response = voucherService.applyVouchers(request);

        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).contains("chưa đạt giá trị đơn hàng tối thiểu");
    }

    @Test
    @DisplayName("3-Slot Stacking: 1 ITEM + 1 ORDER + 1 SHIPPING stack correctly")
    void testItemOrderShippingSlotStacking() {
        when(voucherRepository.findByCodeIgnoreCase("SKINCARE10")).thenReturn(Optional.of(skincareCategoryVoucher));
        when(voucherRepository.findByActiveTrue()).thenReturn(List.of(percentOrderVoucher, freeShipHanoiVoucher));

        VoucherCartItemDto serumItem = VoucherCartItemDto.builder()
                .productId(101L)
                .categoryId(2L) // subcategory of Skincare (1L)
                .price(new BigDecimal("300000.00"))
                .quantity(1)
                .build();

        VoucherCartItemDto shirtItem = VoucherCartItemDto.builder()
                .productId(202L)
                .categoryId(3L) // Fashion
                .price(new BigDecimal("200000.00"))
                .quantity(1)
                .build();

        VoucherApplyRequestDto request = VoucherApplyRequestDto.builder()
                .itemVoucherCode("SKINCARE10")
                .subtotal(new BigDecimal("500000.00"))
                .shippingFee(new BigDecimal("30000.00"))
                .items(List.of(serumItem, shirtItem))
                .customerCityName("Hà Nội")
                .customerDistrictName("Quận Ba Đình")
                .autoApply(true)
                .build();

        VoucherApplyResponseDto response = voucherService.applyVouchers(request);

        assertThat(response.isValid()).isTrue();
        // Item discount: 10% of 300k (serum only) = 30k
        assertThat(response.getItemDiscount()).isEqualByComparingTo("30000.00");
        // Remaining subtotal = 500k - 30k = 470k. Order discount: 5% of 470k = 23.5k
        assertThat(response.getOrderDiscount()).isEqualByComparingTo("23500.00");
        // Shipping discount: 30k (Ba Dinh is in Hanoi Center)
        assertThat(response.getShippingDiscount()).isEqualByComparingTo("30000.00");

        // Subtotal discount = 30k + 23.5k = 53.5k
        assertThat(response.getSubtotalDiscount()).isEqualByComparingTo("53500.00");
        // Final subtotal = 500k - 53.5k = 446.5k
        assertThat(response.getFinalSubtotal()).isEqualByComparingTo("446500.00");
        assertThat(response.getFinalShippingFee()).isEqualByComparingTo("0.00");
        assertThat(response.getFinalTotal()).isEqualByComparingTo("446500.00");
        assertThat(response.getAppliedVouchers()).hasSize(3);
    }

    @Test
    @DisplayName("Hanoi Center FreeShip: Accepted for central districts (Hoan Kiem, Ba Dinh, Cau Giay)")
    void testHanoiCenterFreeShip_Valid() {
        when(voucherRepository.findByCodeIgnoreCase("HANOIFREE")).thenReturn(Optional.of(freeShipHanoiVoucher));

        VoucherApplyRequestDto request = VoucherApplyRequestDto.builder()
                .shippingVoucherCode("HANOIFREE")
                .subtotal(new BigDecimal("300000.00"))
                .shippingFee(new BigDecimal("30000.00"))
                .customerCityName("Thành phố Hà Nội")
                .customerDistrictName("Quận Cầu Giấy")
                .build();

        VoucherApplyResponseDto response = voucherService.applyVouchers(request);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getShippingDiscount()).isEqualByComparingTo("30000.00");
        assertThat(response.getFinalShippingFee()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Hanoi Center FreeShip: Rejected for Long Biên district")
    void testHanoiCenterFreeShip_LongBienRejected() {
        when(voucherRepository.findByCodeIgnoreCase("HANOIFREE")).thenReturn(Optional.of(freeShipHanoiVoucher));

        VoucherApplyRequestDto request = VoucherApplyRequestDto.builder()
                .shippingVoucherCode("HANOIFREE")
                .subtotal(new BigDecimal("300000.00"))
                .shippingFee(new BigDecimal("30000.00"))
                .customerCityName("Hà Nội")
                .customerDistrictName("Quận Long Biên")
                .build();

        VoucherApplyResponseDto response = voucherService.applyVouchers(request);

        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).contains("chỉ áp dụng cho địa chỉ tại 11 quận nội thành Hà Nội");
    }

    @Test
    @DisplayName("Hanoi Center FreeShip: Rejected for outside provinces (TP. Hồ Chí Minh / Đà Nẵng)")
    void testHanoiCenterFreeShip_OutsideProvinceRejected() {
        when(voucherRepository.findByCodeIgnoreCase("HANOIFREE")).thenReturn(Optional.of(freeShipHanoiVoucher));

        VoucherApplyRequestDto request = VoucherApplyRequestDto.builder()
                .shippingVoucherCode("HANOIFREE")
                .subtotal(new BigDecimal("300000.00"))
                .shippingFee(new BigDecimal("30000.00"))
                .customerCityName("TP. Hồ Chí Minh")
                .customerDistrictName("Quận 1")
                .build();

        VoucherApplyResponseDto response = voucherService.applyVouchers(request);

        assertThat(response.isValid()).isFalse();
        assertThat(response.getMessage()).contains("chỉ áp dụng cho địa chỉ tại 11 quận nội thành Hà Nội");
    }

    @Test
    @DisplayName("Invariant validation: Creating PRODUCT scope with empty product list throws exception")
    void testScopeInvariantValidation() {
        VoucherDTO dto = VoucherDTO.builder()
                .code("INVALID_PROD")
                .name("Invalid Product Voucher")
                .type(VoucherType.DISCOUNT_PERCENT)
                .scope(VoucherScope.PRODUCT)
                .applicableProductIds(Set.of())
                .value(new BigDecimal("10.00"))
                .build();

        assertThatThrownBy(() -> voucherService.createVoucher(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voucher theo sản phẩm phải chọn ít nhất một sản phẩm.");
    }

    @Test
    @DisplayName("Invariant validation: Non-FreeShip voucher with GeoScope throws exception")
    void testGeoScopeInvariantValidation() {
        VoucherDTO dto = VoucherDTO.builder()
                .code("INVALID_GEO")
                .name("Invalid Geo Voucher")
                .type(VoucherType.DISCOUNT_PERCENT)
                .geoScope(GeoScope.HANOI_CENTER)
                .value(new BigDecimal("10.00"))
                .build();

        assertThatThrownBy(() -> voucherService.createVoucher(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Giới hạn khu vực (GeoScope) chỉ áp dụng cho mã Miễn phí vận chuyển");
    }

    @Test
    @DisplayName("Atomic quota recording calls incrementUsedCountAtomic")
    void testAtomicUsageLimitDeduction() {
        when(voucherRepository.findByCodeIgnoreCase("SOBUAUTO5")).thenReturn(Optional.of(percentOrderVoucher));
        when(voucherRepository.incrementUsedCountAtomic(1L)).thenReturn(1);

        voucherService.recordVoucherUsage(null, "SOBUAUTO5", null);

        verify(voucherRepository).incrementUsedCountAtomic(1L);
    }
}
