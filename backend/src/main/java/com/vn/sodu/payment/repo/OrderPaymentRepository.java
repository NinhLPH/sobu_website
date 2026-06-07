package com.vn.sodu.payment.repo;

import com.vn.sodu.payment.OrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {

    Optional<OrderPayment> findByPaymentCode(String paymentCode);

    Optional<OrderPayment> findByProviderReference(String providerReference);

    List<OrderPayment> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
