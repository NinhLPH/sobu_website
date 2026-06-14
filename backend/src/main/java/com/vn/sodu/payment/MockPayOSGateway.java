package com.vn.sodu.payment;

import com.vn.sodu.order.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "payos.gateway-mode", havingValue = "mock", matchIfMissing = true)
public class MockPayOSGateway implements PayOSGateway {

    private static final String PROVIDER_PREFIX = "mock-payos";
    private static final long PAYMENT_TTL_MINUTES = 5;
    private final Map<Long, PayOSPaymentStatusSnapshot> paymentStatuses = new ConcurrentHashMap<>();

    @Override
    public String providerName() {
        return "PAYOS_MOCK";
    }

    @Override
    public PayOSCheckoutSession createCheckout(Order order, OrderPayment payment) {
        String orderCode = order == null || order.getOrderCode() == null ? "unknown-order" : order.getOrderCode();
        String paymentCode = payment == null || payment.getPaymentCode() == null ? "unknown-payment" : payment.getPaymentCode();
        String providerReference = PROVIDER_PREFIX + "-" + paymentCode.toLowerCase();
        String checkoutUrl = "https://mock-payos.local/checkout/" + paymentCode;
        String qrCode = "MOCKQR:" + orderCode + ":" + paymentCode;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PAYMENT_TTL_MINUTES);
        if (payment != null && payment.getProviderOrderCode() != null) {
            paymentStatuses.put(
                    payment.getProviderOrderCode(),
                    new PayOSPaymentStatusSnapshot(
                            payment.getProviderOrderCode(),
                            PaymentStatus.PENDING,
                            providerReference,
                            expiresAt,
                            "PENDING"
                    )
            );
        }
        return new PayOSCheckoutSession(
                providerReference,
                checkoutUrl,
                qrCode,
                expiresAt
        );
    }

    @Override
    public PayOSPaymentStatusSnapshot getPaymentStatus(Long providerOrderCode) {
        if (providerOrderCode == null) {
            return null;
        }
        PayOSPaymentStatusSnapshot snapshot = paymentStatuses.get(providerOrderCode);
        if (snapshot == null) {
            return null;
        }
        if (snapshot.status() == PaymentStatus.PENDING
                && snapshot.expiresAt() != null
                && snapshot.expiresAt().isBefore(LocalDateTime.now())) {
            snapshot = new PayOSPaymentStatusSnapshot(
                    snapshot.providerOrderCode(),
                    PaymentStatus.EXPIRED,
                    snapshot.providerReference(),
                    snapshot.expiresAt(),
                    "EXPIRED"
            );
            paymentStatuses.put(providerOrderCode, snapshot);
        }
        return snapshot;
    }

    public void markPaid(Long providerOrderCode) {
        updateStatus(providerOrderCode, PaymentStatus.PAID, "PAID");
    }

    public void markExpired(Long providerOrderCode) {
        updateStatus(providerOrderCode, PaymentStatus.EXPIRED, "EXPIRED");
    }

    private void updateStatus(Long providerOrderCode, PaymentStatus status, String rawStatus) {
        PayOSPaymentStatusSnapshot existing = paymentStatuses.get(providerOrderCode);
        if (existing == null) {
            paymentStatuses.put(providerOrderCode, new PayOSPaymentStatusSnapshot(
                    providerOrderCode,
                    status,
                    null,
                    null,
                    rawStatus
            ));
            return;
        }
        paymentStatuses.put(providerOrderCode, new PayOSPaymentStatusSnapshot(
                existing.providerOrderCode(),
                status,
                existing.providerReference(),
                existing.expiresAt(),
                rawStatus
        ));
    }
}
