package com.vn.sodu.payment;

import com.vn.sodu.order.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MockPayOSGateway implements PayOSGateway {

    private static final String PROVIDER_PREFIX = "mock-payos";

    @Override
    public PayOSCheckoutSession createCheckout(Order order, OrderPayment payment) {
        String orderCode = order == null || order.getOrderCode() == null ? "unknown-order" : order.getOrderCode();
        String paymentCode = payment == null || payment.getPaymentCode() == null ? "unknown-payment" : payment.getPaymentCode();
        String providerReference = PROVIDER_PREFIX + "-" + paymentCode.toLowerCase();
        String checkoutUrl = "https://mock-payos.local/checkout/" + paymentCode;
        String qrCode = "MOCKQR:" + orderCode + ":" + paymentCode;
        return new PayOSCheckoutSession(
                providerReference,
                checkoutUrl,
                qrCode,
                LocalDateTime.now().plusMinutes(30)
        );
    }
}
