package com.vn.sodu.order.policy;

import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.request.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTransitionPolicyTest {

    private OrderTransitionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new OrderTransitionPolicy();
    }

    @Test
    @DisplayName("Normal order allows PROCESSING to SHIPPED and PROCESSING to CANCELLED")
    void testNormalOrderProcessingTransitions() {
        assertThat(policy.canTransition(OrderType.NORMAL, OrderStatus.PROCESSING, OrderStatus.SHIPPED)).isTrue();
        assertThat(policy.canTransition(OrderType.NORMAL, OrderStatus.PROCESSING, OrderStatus.CANCELLED)).isTrue();

        policy.validateTransition(OrderType.NORMAL, OrderStatus.PROCESSING, OrderStatus.SHIPPED);
        policy.validateTransition(OrderType.NORMAL, OrderStatus.PROCESSING, OrderStatus.CANCELLED);

        Set<OrderStatus> allowed = policy.allowedTransitions(OrderType.NORMAL, OrderStatus.PROCESSING);
        assertThat(allowed).containsExactlyInAnyOrder(OrderStatus.SHIPPED, OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Normal order allows DELIVERED to RETURNED and RETURNED to CANCELLED")
    void testNormalOrderDeliveredAndReturnedTransitions() {
        assertThat(policy.canTransition(OrderType.NORMAL, OrderStatus.DELIVERED, OrderStatus.RETURNED)).isTrue();
        assertThat(policy.canTransition(OrderType.NORMAL, OrderStatus.RETURNED, OrderStatus.CANCELLED)).isTrue();

        Set<OrderStatus> allowedFromDelivered = policy.allowedTransitions(OrderType.NORMAL, OrderStatus.DELIVERED);
        assertThat(allowedFromDelivered).containsExactly(OrderStatus.RETURNED);
    }

    @Test
    @DisplayName("Normal order rejects invalid leaps like NEW to DELIVERED or CANCELLED to PROCESSING")
    void testNormalOrderInvalidTransitions() {
        assertThat(policy.canTransition(OrderType.NORMAL, OrderStatus.NEW, OrderStatus.DELIVERED)).isFalse();
        assertThatThrownBy(() -> policy.validateTransition(OrderType.NORMAL, OrderStatus.NEW, OrderStatus.DELIVERED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid order transition from NEW to DELIVERED for NORMAL order");

        assertThat(policy.canTransition(OrderType.NORMAL, OrderStatus.CANCELLED, OrderStatus.PROCESSING)).isFalse();
        assertThatThrownBy(() -> policy.validateTransition(OrderType.NORMAL, OrderStatus.CANCELLED, OrderStatus.PROCESSING))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Preorder allows staged deposit and final transitions")
    void testPreorderTransitions() {
        assertThat(policy.canTransition(OrderType.PREORDER, OrderStatus.WAITING_DEPOSIT, OrderStatus.DEPOSIT_PAID)).isTrue();
        assertThat(policy.canTransition(OrderType.PREORDER, OrderStatus.DEPOSIT_PAID, OrderStatus.READY_FOR_FINAL_PAYMENT)).isTrue();
        assertThat(policy.canTransition(OrderType.PREORDER, OrderStatus.READY_FOR_FINAL_PAYMENT, OrderStatus.PROCESSING)).isTrue();
        assertThat(policy.canTransition(OrderType.PREORDER, OrderStatus.PROCESSING, OrderStatus.SHIPPED)).isTrue();
        assertThat(policy.canTransition(OrderType.PREORDER, OrderStatus.PROCESSING, OrderStatus.CANCELLED)).isTrue();

        assertThat(policy.canTransition(OrderType.PREORDER, OrderStatus.WAITING_DEPOSIT, OrderStatus.SHIPPED)).isFalse();
    }

    @Test
    @DisplayName("Identical from and to status returns true")
    void testIdenticalTransitions() {
        assertThat(policy.canTransition(OrderType.NORMAL, OrderStatus.PROCESSING, OrderStatus.PROCESSING)).isTrue();
    }
}
