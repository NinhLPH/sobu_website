package com.vn.sodu.order.policy;

import com.vn.sodu.order.OrderStatus;
import com.vn.sodu.request.OrderType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class OrderTransitionPolicy {

    private static final Map<OrderStatus, Set<OrderStatus>> NORMAL_MATRIX = buildNormalMatrix();
    private static final Map<OrderStatus, Set<OrderStatus>> PREORDER_MATRIX = buildPreorderMatrix();

    public boolean canTransition(OrderType type, OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        Map<OrderStatus, Set<OrderStatus>> matrix = (type == OrderType.PREORDER) ? PREORDER_MATRIX : NORMAL_MATRIX;
        return matrix.getOrDefault(from, EnumSet.noneOf(OrderStatus.class)).contains(to);
    }

    public void validateTransition(OrderType type, OrderStatus from, OrderStatus to) {
        if (!canTransition(type, from, to)) {
            String orderTypeName = type != null ? type.name() : "UNKNOWN";
            throw new IllegalStateException(
                    String.format("Invalid order transition from %s to %s for %s order", from, to, orderTypeName)
            );
        }
    }

    public Set<OrderStatus> allowedTransitions(OrderType type, OrderStatus from) {
        if (from == null) {
            return Set.of();
        }
        Map<OrderStatus, Set<OrderStatus>> matrix = (type == OrderType.PREORDER) ? PREORDER_MATRIX : NORMAL_MATRIX;
        return Set.copyOf(matrix.getOrDefault(from, EnumSet.noneOf(OrderStatus.class)));
    }

    private static Map<OrderStatus, Set<OrderStatus>> buildNormalMatrix() {
        EnumMap<OrderStatus, Set<OrderStatus>> matrix = new EnumMap<>(OrderStatus.class);

        matrix.put(OrderStatus.NEW, EnumSet.of(
                OrderStatus.PROCESSING,
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.PROCESSING, EnumSet.of(
                OrderStatus.SHIPPED,
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.SHIPPED, EnumSet.of(
                OrderStatus.DELIVERED,
                OrderStatus.FAILED,
                OrderStatus.RETURNED,
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.FAILED, EnumSet.of(
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED,
                OrderStatus.RETURNED,
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.DELIVERED, EnumSet.of(
                OrderStatus.RETURNED
        ));

        matrix.put(OrderStatus.RETURNED, EnumSet.of(
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

        return Map.copyOf(matrix);
    }

    private static Map<OrderStatus, Set<OrderStatus>> buildPreorderMatrix() {
        EnumMap<OrderStatus, Set<OrderStatus>> matrix = new EnumMap<>(OrderStatus.class);

        matrix.put(OrderStatus.WAITING_DEPOSIT, EnumSet.of(
                OrderStatus.DEPOSIT_PAID,
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.DEPOSIT_PAID, EnumSet.of(
                OrderStatus.READY_FOR_FINAL_PAYMENT,
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.READY_FOR_FINAL_PAYMENT, EnumSet.of(
                OrderStatus.PROCESSING,
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.PROCESSING, EnumSet.of(
                OrderStatus.SHIPPED,
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.SHIPPED, EnumSet.of(
                OrderStatus.DELIVERED,
                OrderStatus.FAILED,
                OrderStatus.RETURNED,
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.FAILED, EnumSet.of(
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED,
                OrderStatus.RETURNED,
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.DELIVERED, EnumSet.of(
                OrderStatus.RETURNED
        ));

        matrix.put(OrderStatus.RETURNED, EnumSet.of(
                OrderStatus.CANCELLED
        ));

        matrix.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

        return Map.copyOf(matrix);
    }
}
