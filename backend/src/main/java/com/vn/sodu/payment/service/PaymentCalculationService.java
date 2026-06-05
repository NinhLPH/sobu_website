package com.vn.sodu.payment.service;

import com.vn.sodu.order.Order;
import com.vn.sodu.payment.OrderPayment;
import com.vn.sodu.payment.PaymentStatus;
import com.vn.sodu.payment.PaymentType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PaymentCalculationService {

    public BigDecimal normalizeMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePaymentAmount(Order order, PaymentType type) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Payment type is required");
        }

        BigDecimal totalAmount = normalizeMoney(order.getTotalAmount());
        BigDecimal depositAmount = normalizeMoney(order.getDepositAmount());
        BigDecimal paidAmount = normalizeMoney(order.getPaidAmount());

        return switch (type) {
            case FULL -> totalAmount;
            case DEPOSIT -> depositAmount.min(totalAmount);
            case FINAL -> totalAmount.subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            case REFUND -> paidAmount;
        };
    }

    public BigDecimal calculatePaidAmount(List<OrderPayment> payments) {
        BigDecimal netPaid = BigDecimal.ZERO;
        if (payments == null) {
            return normalizeMoney(netPaid);
        }

        for (OrderPayment payment : payments) {
            if (payment == null || payment.getAmount() == null) {
                continue;
            }

            BigDecimal amount = normalizeMoney(payment.getAmount());
            PaymentStatus status = payment.getStatus();
            PaymentType type = payment.getType();

            if (type == PaymentType.REFUND) {
                if (status == PaymentStatus.REFUNDED || status == PaymentStatus.PAID) {
                    netPaid = netPaid.subtract(amount);
                }
                continue;
            }

            if (status == PaymentStatus.PAID) {
                netPaid = netPaid.add(amount);
            }
        }

        return normalizeMoney(netPaid.max(BigDecimal.ZERO));
    }

    public BigDecimal calculateRemainingAmount(Order order, List<OrderPayment> payments) {
        if (order == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal totalAmount = normalizeMoney(order.getTotalAmount());
        BigDecimal paidAmount = calculatePaidAmount(payments);
        return normalizeMoney(totalAmount.subtract(paidAmount).max(BigDecimal.ZERO));
    }

    public PaymentStatus calculateOrderPaymentStatus(Order order, List<OrderPayment> payments) {
        if (order == null || payments == null || payments.isEmpty()) {
            return PaymentStatus.PENDING;
        }

        BigDecimal totalAmount = normalizeMoney(order.getTotalAmount());
        BigDecimal paidAmount = calculatePaidAmount(payments);

        if (totalAmount.signum() > 0 && paidAmount.compareTo(totalAmount) >= 0) {
            return PaymentStatus.PAID;
        }

        boolean hasPending = payments.stream().anyMatch(payment -> payment != null && payment.getStatus() == PaymentStatus.PENDING);
        if (hasPending || paidAmount.signum() > 0) {
            return PaymentStatus.PENDING;
        }

        boolean hasFailed = payments.stream().anyMatch(payment -> payment != null && payment.getStatus() == PaymentStatus.FAILED);
        if (hasFailed) {
            return PaymentStatus.FAILED;
        }

        boolean hasExpired = payments.stream().anyMatch(payment -> payment != null && payment.getStatus() == PaymentStatus.EXPIRED);
        if (hasExpired) {
            return PaymentStatus.EXPIRED;
        }

        boolean hasCancelled = payments.stream().anyMatch(payment -> payment != null && payment.getStatus() == PaymentStatus.CANCELLED);
        if (hasCancelled) {
            return PaymentStatus.CANCELLED;
        }

        boolean hasRefunded = payments.stream().anyMatch(payment -> payment != null && payment.getStatus() == PaymentStatus.REFUNDED);
        if (hasRefunded) {
            return PaymentStatus.REFUNDED;
        }

        return PaymentStatus.PENDING;
    }

    public void applyOrderPaymentState(Order order, List<OrderPayment> payments) {
        if (order == null) {
            return;
        }
        order.setPaidAmount(calculatePaidAmount(payments));
        order.setRemainingAmount(calculateRemainingAmount(order, payments));
        order.setPaymentStatus(calculateOrderPaymentStatus(order, payments));
    }
}
