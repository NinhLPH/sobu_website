package com.vn.sodu.payment.service;

import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PayOSGateway;
import com.vn.sodu.payment.PayOSPaymentStatusSnapshot;
import com.vn.sodu.payment.PayOSProperties;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSPaymentReconciliationService {

    private final OrderPaymentRepository orderPaymentRepository;
    private final PayOSGateway payOSGateway;
    private final PayOSProperties payOSProperties;
    private final PaymentService paymentService;

    @Scheduled(
            initialDelayString = "#{@payOSProperties.reconciliation.initialDelayMs}",
            fixedDelayString = "#{@payOSProperties.reconciliation.fixedDelayMs}"
    )
    public void scheduledReconcilePendingOnlinePayments() {
        reconcilePendingOnlinePayments();
    }

    public PaymentReconciliationResult reconcilePendingOnlinePayments() {
        if (!payOSProperties.getReconciliation().isEnabled()) {
            return new PaymentReconciliationResult(0, 0, 0, 0, 0, 0);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleCutoff = now.minusSeconds(Math.max(0L, payOSProperties.getReconciliation().getStaleAfterSeconds()));
        int batchSize = Math.max(1, payOSProperties.getReconciliation().getBatchSize());
        List<OrderPayment> candidates = orderPaymentRepository.findByStatusInAndPaymentMethodOrderByCreatedAtAsc(
                EnumSet.of(PaymentStatus.PENDING, PaymentStatus.FAILED),
                PaymentMethod.ONLINE,
                PageRequest.of(0, batchSize)
        );

        int skipped = 0;
        int paid = 0;
        int expired = 0;
        int providerStatusUnavailable = 0;
        int failed = 0;
        for (OrderPayment payment : candidates) {
            if (payment == null || payment.getProviderOrderCode() == null || !isCandidate(payment, staleCutoff, now)) {
                skipped++;
                continue;
            }
            try {
                PayOSPaymentStatusSnapshot snapshot = payOSGateway.getPaymentStatus(payment.getProviderOrderCode());
                if (snapshot == null) {
                    if (isExpired(payment, now)) {
                        paymentService.markPaymentExpired(payment.getPaymentCode(), "Payment session expired");
                        expired++;
                    } else {
                        providerStatusUnavailable++;
                    }
                    continue;
                }
                if (snapshot.status() == PaymentStatus.PAID) {
                    paymentService.markPaymentPaid(payment.getPaymentCode());
                    paid++;
                } else if (snapshot.status() == PaymentStatus.EXPIRED) {
                    paymentService.markPaymentExpired(payment.getPaymentCode(), "Payment session expired");
                    expired++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException ex) {
                failed++;
                log.error("PayOS reconciliation failed for paymentCode={} providerOrderCode={}",
                        payment.getPaymentCode(), payment.getProviderOrderCode(), ex);
            }
        }
        return new PaymentReconciliationResult(candidates.size(), skipped, paid, expired, providerStatusUnavailable, failed);
    }

    private boolean isCandidate(OrderPayment payment, LocalDateTime staleCutoff, LocalDateTime now) {
        if (payment.getCreatedAt() != null && payment.getCreatedAt().isAfter(staleCutoff)) {
            return isExpired(payment, now);
        }
        return true;
    }

    private boolean isExpired(OrderPayment payment, LocalDateTime now) {
        return payment.getExpiresAt() != null && !payment.getExpiresAt().isAfter(now);
    }
}
