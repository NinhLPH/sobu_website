package com.vn.sodu.product.service;

import com.vn.sodu.product.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPricingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 25, 12, 0);

    @Test
    void returnsSalePriceInsideValidityWindow() {
        Product product = saleProduct();
        product.setSaleValidFrom(NOW.minusDays(1));
        product.setSaleValidThrough(NOW.plusDays(1));

        assertThat(ProductPricing.isSaleActive(product, NOW)).isTrue();
        assertThat(ProductPricing.effectivePrice(product, NOW)).isEqualByComparingTo("80000");
        assertThat(ProductPricing.displayOldPrice(product, NOW)).isEqualByComparingTo("100000");
    }

    @Test
    void returnsRegularPriceBeforeOrAfterValidityWindow() {
        Product scheduled = saleProduct();
        scheduled.setSaleValidFrom(NOW.plusMinutes(1));
        Product expired = saleProduct();
        expired.setSaleValidThrough(NOW.minusMinutes(1));

        assertThat(ProductPricing.isSaleActive(scheduled, NOW)).isFalse();
        assertThat(ProductPricing.isSaleActive(expired, NOW)).isFalse();
        assertThat(ProductPricing.effectivePrice(scheduled, NOW)).isEqualByComparingTo("100000");
        assertThat(ProductPricing.effectivePrice(expired, NOW)).isEqualByComparingTo("100000");
    }

    @Test
    void rejectsInvalidPricePairAsSale() {
        Product product = saleProduct();
        product.setOldPrice(new BigDecimal("70000"));

        assertThat(ProductPricing.hasValidDiscount(product)).isFalse();
        assertThat(ProductPricing.isSaleActive(product, NOW)).isFalse();
        assertThat(ProductPricing.effectivePrice(product, NOW)).isEqualByComparingTo("80000");
        assertThat(ProductPricing.displayOldPrice(product, NOW)).isNull();
    }

    private Product saleProduct() {
        Product product = new Product();
        product.setRetailPrice(new BigDecimal("80000"));
        product.setOldPrice(new BigDecimal("100000"));
        return product;
    }
}
