package com.vn.sodu.payment.service;

import com.vn.sodu.order.NhanhSyncStage;
import com.vn.sodu.order.Order;
import com.vn.sodu.order.OrderReadyForSyncEvent;
import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.order.OrderSyncStatus;
import com.vn.sodu.order.repo.OrderRepository;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PayOSGateway;
import com.vn.sodu.payment.PayOSPaymentStatusSnapshot;
import com.vn.sodu.payment.PayOSProperties;
import com.vn.sodu.payment.PaymentMethod;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.payment.dto.PaymentReconciliationResult;
import com.vn.sodu.payment.repo.OrderPaymentRepository;
import com.vn.sodu.request.OrderType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({
        PaymentService.class,
        PaymentReconciliationService.class,
        PaymentCalculationService.class,
        PaymentReconciliationTransactionIntegrationTest.TestConfig.class
})
class PaymentReconciliationTransactionIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderPaymentRepository orderPaymentRepository;

    @Autowired
    private PaymentReconciliationService reconciliationService;

    @Autowired
    private EventRecorder eventRecorder;

    @MockBean
    private PayOSGateway payOSGateway;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void paidProviderStatusUsesPaymentServiceTransactionAndPublishesSyncEvent() {
        Order order = orderRepository.save(Order.builder()
                .orderCode("SOBU-INT-RECON-1")
                .appOrderId("SOBU-INT-RECON-1")
                .type(OrderType.NORMAL)
                .status(OrderStatus.NEW)
                .syncStatus(OrderSyncStatus.PENDING)
                .nhanhSyncStage(NhanhSyncStage.NONE)
                .totalAmount(new BigDecimal("15000"))
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("15000"))
                .paymentStatus(PaymentStatus.PENDING)
                .build());
        OrderPayment payment = orderPaymentRepository.save(OrderPayment.builder()
                .order(order)
                .paymentCode("SOBU-PAY-INT-RECON-1")
                .providerOrderCode(900001L)
                .type(PaymentType.FULL)
                .paymentMethod(PaymentMethod.ONLINE)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("15000"))
                .provider("PAYOS")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());
        when(payOSGateway.getPaymentStatus(900001L)).thenReturn(new PayOSPaymentStatusSnapshot(
                900001L,
                PaymentStatus.PAID,
                "reference-900001",
                null,
                "PAID"
        ));

        PaymentReconciliationResult result = reconciliationService.reconcileBatch();

        assertThat(result.markedPaid()).isEqualTo(1);
        assertThat(orderPaymentRepository.findById(payment.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(eventRecorder.events()).contains(new OrderReadyForSyncEvent(order.getId(), payment.getPaymentCode()));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        PayOSProperties payOSProperties() {
            PayOSProperties properties = new PayOSProperties();
            properties.getReconciliation().setStaleAfterSeconds(0);
            properties.getReconciliation().setBatchSize(10);
            return properties;
        }

        @Bean
        EventRecorder eventRecorder() {
            return new EventRecorder();
        }
    }

    static class EventRecorder {
        private final List<OrderReadyForSyncEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        void record(OrderReadyForSyncEvent event) {
            events.add(event);
        }

        List<OrderReadyForSyncEvent> events() {
            return events;
        }
    }
}
