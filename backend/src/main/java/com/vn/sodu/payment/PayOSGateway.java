package com.vn.sodu.payment;

import com.vn.sodu.order.Order;

public interface PayOSGateway {

    default String providerName() {
        return "PAYOS";
    }

    PayOSCheckoutSession createCheckout(Order order, OrderPayment payment);

    default PayOSPaymentStatusSnapshot getPaymentStatus(Long providerOrderCode) {
        return null;
    }
}
