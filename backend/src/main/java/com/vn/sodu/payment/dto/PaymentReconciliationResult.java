package com.vn.sodu.payment.dto;

/**
 * Summary of one bounded reconciliation pass against PayOS.
 */
public record PaymentReconciliationResult(
        int scanned,
        int skipped,
        int markedPaid,
        int markedExpired,
        int providerStatusUnavailable,
        int errors
) {
}
