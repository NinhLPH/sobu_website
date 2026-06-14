package com.vn.sodu.payment.repo;

import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {

    Optional<OrderPayment> findByPaymentCode(String paymentCode);

    Optional<OrderPayment> findByProviderReference(String providerReference);

    Optional<OrderPayment> findByProviderOrderCode(Long providerOrderCode);

    List<OrderPayment> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    List<OrderPayment> findByStatusInAndPaymentMethodOrderByCreatedAtAsc(
            Collection<PaymentStatus> statuses,
            PaymentMethod paymentMethod,
            Pageable pageable
    );
}
