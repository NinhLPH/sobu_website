package com.vn.sodu.payment;

import java.time.LocalDateTime;

public record PayOSCheckoutSession(
        String providerReference,
        String checkoutUrl,
        String qrCode,
        LocalDateTime expiresAt
) {
}
