package com.vn.sodu.payment.service;

import com.vn.sodu.order.Order;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import com.vn.sodu.request.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCalculationServiceTest {

    private final PaymentCalculationService service = new PaymentCalculationService();

    @Test
    void calculatePaymentAmountUsesOrderRulesByType() {
        Order order = Order.builder()
                .type(OrderType.PREORDER)
                .totalAmount(new BigDecimal("1000"))
                .depositAmount(new BigDecimal("300"))
                .paidAmount(new BigDecimal("300"))
                .build();

        assertThat(service.calculatePaymentAmount(order, PaymentType.FULL)).isEqualByComparingTo("1000.00");
        assertThat(service.calculatePaymentAmount(order, PaymentType.DEPOSIT)).isEqualByComparingTo("300.00");
        assertThat(service.calculatePaymentAmount(order, PaymentType.FINAL)).isEqualByComparingTo("700.00");
        assertThat(service.calculatePaymentAmount(order, PaymentType.REFUND)).isEqualByComparingTo("300.00");
    }

    @Test
    void applyOrderPaymentStateAggregatesPaidAndRemainingAmounts() {
        Order order = Order.builder()
                .type(OrderType.NORMAL)
                .totalAmount(new BigDecimal("1000"))
                .depositAmount(BigDecimal.ZERO)
                .build();
        List<OrderPayment> payments = List.of(
                OrderPayment.builder()
                        .type(PaymentType.DEPOSIT)
                        .status(PaymentStatus.PAID)
                        .amount(new BigDecimal("300"))
                        .build(),
                OrderPayment.builder()
                        .type(PaymentType.FINAL)
                        .status(PaymentStatus.PENDING)
                        .amount(new BigDecimal("700"))
                        .build()
        );

        service.applyOrderPaymentState(order, payments);

        assertThat(order.getPaidAmount()).isEqualByComparingTo("300.00");
        assertThat(order.getRemainingAmount()).isEqualByComparingTo("700.00");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void applyOrderPaymentStateMarksOrderPaidWhenCollectedAmountMatchesTotal() {
        Order order = Order.builder()
                .type(OrderType.NORMAL)
                .totalAmount(new BigDecimal("1000"))
                .build();
        List<OrderPayment> payments = List.of(
                OrderPayment.builder()
                        .type(PaymentType.FULL)
                        .status(PaymentStatus.PAID)
                        .amount(new BigDecimal("1000"))
                        .build()
        );

        service.applyOrderPaymentState(order, payments);

        assertThat(order.getPaidAmount()).isEqualByComparingTo("1000.00");
        assertThat(order.getRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }
}
