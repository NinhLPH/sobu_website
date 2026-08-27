package com.vn.sodu.product.service;

import com.vn.sodu.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Single source of truth for product sale pricing.
 * retailPrice is the configured sale price and oldPrice is the regular price.
 */
public final class ProductPricing {

    public record PriceView(BigDecimal price, BigDecimal oldPrice, boolean onSale) {
    }

    private ProductPricing() {
    }

    public static boolean hasValidDiscount(Product product) {
        if (product == null) {
            return false;
        }
        BigDecimal price = product.getRetailPrice();
        BigDecimal oldPrice = product.getOldPrice();
        return price != null
                && oldPrice != null
                && price.compareTo(BigDecimal.ZERO) >= 0
                && oldPrice.compareTo(price) > 0;
    }

    public static boolean isSaleActive(Product product) {
        return isSaleActive(product, LocalDateTime.now());
    }

    static boolean isSaleActive(Product product, LocalDateTime now) {
        if (!hasValidDiscount(product)) {
            return false;
        }
        return (product.getSaleValidFrom() == null || !now.isBefore(product.getSaleValidFrom()))
                && (product.getSaleValidThrough() == null || !now.isAfter(product.getSaleValidThrough()));
    }

    public static BigDecimal effectivePrice(Product product) {
        return resolve(product).price();
    }

    static BigDecimal effectivePrice(Product product, LocalDateTime now) {
        if (product == null) {
            return null;
        }
        if (product.getRetailPrice() != null) {
            return product.getRetailPrice();
        }
        return product.getOldPrice();
    }

    public static BigDecimal displayOldPrice(Product product) {
        return resolve(product).oldPrice();
    }

    static BigDecimal displayOldPrice(Product product, LocalDateTime now) {
        return isSaleActive(product, now) ? product.getOldPrice() : null;
    }

    public static PriceView resolve(Product product) {
        return resolve(product, LocalDateTime.now());
    }

    static PriceView resolve(Product product, LocalDateTime now) {
        if (product == null) {
            return new PriceView(null, null, false);
        }
        boolean onSale = isSaleActive(product, now);
        BigDecimal price = effectivePrice(product, now);
        BigDecimal oldPrice = onSale ? product.getOldPrice() : null;
        return new PriceView(price, oldPrice, onSale);
    }
}
