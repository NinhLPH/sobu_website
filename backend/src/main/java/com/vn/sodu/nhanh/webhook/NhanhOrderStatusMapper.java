package com.vn.sodu.nhanh.webhook;

import com.vn.sodu.order.OrderStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NhanhOrderStatusMapper {

    public OrderStatus mapToLocal(int nhanhCode) {
        return switch (nhanhCode) {
            case 54 -> OrderStatus.NEW;
            case 55, 57 -> OrderStatus.WAITING_DEPOSIT;
            case 56, 73 -> OrderStatus.PROCESSING;
            case 58, 63, 64, 68 -> OrderStatus.CANCELLED;
            case 59 -> OrderStatus.SHIPPED;
            case 60 -> OrderStatus.DELIVERED;
            case 61 -> OrderStatus.FAILED;
            case 71, 72, 74 -> OrderStatus.RETURNED;
            default -> null;
        };
    }
}
