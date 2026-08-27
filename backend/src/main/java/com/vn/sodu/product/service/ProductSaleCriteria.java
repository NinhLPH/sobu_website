package com.vn.sodu.product.service;

import com.vn.sodu.product.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

final class ProductSaleCriteria {

    private ProductSaleCriteria() {
    }

    static Predicate validDiscount(Root<Product> root, CriteriaBuilder cb) {
        Expression<BigDecimal> price = root.get("retailPrice");
        Expression<BigDecimal> oldPrice = root.get("oldPrice");
        return cb.and(
                cb.isNotNull(price),
                cb.isNotNull(oldPrice),
                cb.greaterThanOrEqualTo(price, BigDecimal.ZERO),
                cb.greaterThan(oldPrice, price)
        );
    }

    static Predicate activeSale(Root<Product> root, CriteriaBuilder cb, LocalDateTime now) {
        return cb.and(
                validDiscount(root, cb),
                cb.or(
                        cb.isNull(root.get("saleValidFrom")),
                        cb.lessThanOrEqualTo(root.get("saleValidFrom"), now)
                ),
                cb.or(
                        cb.isNull(root.get("saleValidThrough")),
                        cb.greaterThanOrEqualTo(root.get("saleValidThrough"), now)
                )
        );
    }

    static Expression<BigDecimal> effectivePrice(Root<Product> root, CriteriaBuilder cb, LocalDateTime now) {
        return cb.coalesce(root.<BigDecimal>get("retailPrice"), root.<BigDecimal>get("oldPrice"));
    }

    static boolean isComputedSort(String sortBy) {
        String normalized = normalize(sortBy);
        return normalized.equals("price")
                || normalized.equals("retailprice")
                || normalized.equals("effectiveprice")
                || normalized.equals("discount")
                || normalized.equals("discountpercent");
    }

    static void applyComputedSort(
            Root<Product> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String sortBy,
            Sort.Direction direction,
            LocalDateTime now
    ) {
        if (!isComputedSort(sortBy) || Long.class.equals(query.getResultType())) {
            return;
        }

        Expression<?> expression;
        String normalized = normalize(sortBy);
        if (normalized.equals("discount") || normalized.equals("discountpercent")) {
            Expression<Number> ratio = cb.quot(
                    cb.diff(root.<BigDecimal>get("oldPrice"), root.<BigDecimal>get("retailPrice")),
                    root.<BigDecimal>get("oldPrice")
            );
            expression = cb.<Number>selectCase()
                    .when(activeSale(root, cb, now), ratio)
                    .otherwise(0);
        } else {
            expression = effectivePrice(root, cb, now);
        }

        query.orderBy(
                direction == Sort.Direction.ASC ? cb.asc(expression) : cb.desc(expression),
                cb.desc(root.get("id"))
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
