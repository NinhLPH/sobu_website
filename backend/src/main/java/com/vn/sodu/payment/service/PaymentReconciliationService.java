package com.vn.sodu.payment.service;

import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PayOSGateway;
import com.vn.sodu.payment.PayOSPaymentStatusSnapshot;
import com.vn.sodu.payment.PayOSProperties;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.dto.PaymentReconciliationResult;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Runs outside PaymentService so updates go through PaymentService's Spring
 * transactional proxy before acquiring pessimistic payment locks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final OrderPaymentRepository orderPaymentRepository;
    private final PayOSGateway payOSGateway;
    private final PayOSProperties payOSProperties;
    private final PaymentService paymentService;

    @Scheduled(
            initialDelayString = "#{@payOSProperties.reconciliation.initialDelayMs}",
            fixedDelayString = "#{@payOSProperties.reconciliation.fixedDelayMs}"
    )
    public void reconcileOnSchedule() {
        if (!payOSProperties.getReconciliation().isEnabled()) {
            return;
        }
        PaymentReconciliationResult result = reconcileBatch();
        log.info(
                "PayOS reconciliation completed: scanned={}, skipped={}, markedPaid={}, markedExpired={}, providerStatusUnavailable={}, errors={}",
                result.scanned(), result.skipped(), result.markedPaid(), result.markedExpired(),
                result.providerStatusUnavailable(), result.errors()
        );
    }

    public PaymentReconciliationResult reconcileBatch() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleCutoff = now.minusSeconds(Math.max(0L, payOSProperties.getReconciliation().getStaleAfterSeconds()));
        int batchSize = Math.max(1, payOSProperties.getReconciliation().getBatchSize());
        List<OrderPayment> candidates = orderPaymentRepository.findByStatusInAndPaymentMethodOrderByCreatedAtAsc(
                EnumSet.of(PaymentStatus.PENDING, PaymentStatus.FAILED),
                PaymentMethod.ONLINE,
                PageRequest.of(0, batchSize)
        );

        Counters counters = new Counters();
        for (OrderPayment payment : candidates) {
            counters.scanned++;
            if (payment == null || payment.getProviderOrderCode() == null || !isCandidate(payment, staleCutoff, now)) {
                counters.skipped++;
                continue;
            }

            reconcileOne(payment, now, counters);
        }
        return counters.toResult();
    }

    private void reconcileOne(OrderPayment payment, LocalDateTime now, Counters counters) {
        PayOSPaymentStatusSnapshot snapshot = null;
        try {
            snapshot = payOSGateway.getPaymentStatus(payment.getProviderOrderCode());
            if (snapshot == null) {
                if (payment.getExpiresAt() != null && !payment.getExpiresAt().isAfter(now)) {
                    paymentService.markPaymentExpired(payment.getPaymentCode(), "Payment session expired");
                    counters.markedExpired++;
                } else {
                    counters.providerStatusUnavailable++;
                }
                return;
            }

            if (snapshot.status() == PaymentStatus.PAID) {
                paymentService.markPaymentPaid(payment.getPaymentCode());
                counters.markedPaid++;
            } else if (snapshot.status() == PaymentStatus.EXPIRED) {
                paymentService.markPaymentExpired(payment.getPaymentCode(), "Payment session expired");
                counters.markedExpired++;
            } else {
                counters.skipped++;
            }
        } catch (RuntimeException ex) {
            counters.errors++;
            log.warn(
                    "PayOS reconciliation failed: paymentCode={}, providerOrderCode={}, providerStatus={}",
                    payment.getPaymentCode(),
                    payment.getProviderOrderCode(),
                    snapshot == null ? null : snapshot.rawStatus(),
                    ex
            );
        }
    }

    private boolean isCandidate(OrderPayment payment, LocalDateTime staleCutoff, LocalDateTime now) {
        if (payment.getCreatedAt() != null && payment.getCreatedAt().isAfter(staleCutoff)) {
            return payment.getExpiresAt() != null && !payment.getExpiresAt().isAfter(now);
        }
        return true;
    }

    private static class Counters {
        private int scanned;
        private int skipped;
        private int markedPaid;
        private int markedExpired;
        private int providerStatusUnavailable;
        private int errors;

        private PaymentReconciliationResult toResult() {
            return new PaymentReconciliationResult(
                    scanned,
                    skipped,
                    markedPaid,
                    markedExpired,
                    providerStatusUnavailable,
                    errors
            );
        }
    }
}
