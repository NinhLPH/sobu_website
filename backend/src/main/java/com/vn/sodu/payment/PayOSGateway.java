package com.vn.sodu.payment;

import com.vn.sodu.order.Order;

public interface PayOSGateway {

    PayOSCheckoutSession createCheckout(Order order, OrderPayment payment);
}
