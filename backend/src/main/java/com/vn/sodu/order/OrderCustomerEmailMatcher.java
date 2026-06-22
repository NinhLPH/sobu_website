package com.vn.sodu.order;

import java.util.Locale;

public final class OrderCustomerEmailMatcher {

    private OrderCustomerEmailMatcher() {
    }

    public static boolean matches(String orderEmail, String accountEmail) {
        String normalizedOrderEmail = normalize(orderEmail);
        String normalizedAccountEmail = normalize(accountEmail);
        return normalizedOrderEmail != null && normalizedOrderEmail.equals(normalizedAccountEmail);
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
