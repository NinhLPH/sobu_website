package com.vn.sodu.payment.service;

public record PaymentReconciliationResult(
        int scanned,
        int skipped,
        int paid,
        int expired,
        int providerStatusUnavailable,
        int failed
) {
}
