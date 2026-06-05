package com.vn.sodu.payment.repo;

import com.vn.sodu.payment.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
}
