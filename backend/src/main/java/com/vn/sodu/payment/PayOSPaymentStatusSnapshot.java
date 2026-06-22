package com.vn.sodu.payment;

import java.time.LocalDateTime;

public record PayOSPaymentStatusSnapshot(
        Long providerOrderCode,
        PaymentStatus status,
        String providerReference,
        LocalDateTime expiresAt,
        String rawStatus
) {
}
