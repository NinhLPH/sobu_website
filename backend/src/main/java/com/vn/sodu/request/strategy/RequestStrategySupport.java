package com.vn.sodu.request.strategy;

import com.vn.sodu.request.dto.CreateRequestDto;
import com.vn.sodu.request.dto.RequestItemDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RequestStrategySupport {

    private RequestStrategySupport() {
    }

    static void requireItems(CreateRequestDto dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one request item is required");
        }
    }

    static void requireCustomRequirements(CreateRequestDto dto) {
        if (dto == null || dto.getCustomRequirements() == null || dto.getCustomRequirements().isBlank()) {
            throw new IllegalArgumentException("Custom requirements are required");
        }
    }

    static BigDecimal sumItems(List<RequestItemDto> items, boolean requirePrice) {
        BigDecimal total = BigDecimal.ZERO;
        for (RequestItemDto item : items) {
            if (item == null) {
                continue;
            }
            BigDecimal price = item.getPrice();
            if (price == null) {
                if (requirePrice) {
                    throw new IllegalArgumentException("Item price is required for NORMAL requests");
                }
                continue;
            }
            BigDecimal line = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(line);
        }
        return scaleMoney(total);
    }

    static BigDecimal percentageOf(BigDecimal base, int percent) {
        if (base == null || base.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return scaleMoney(base.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    static BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal scaleMoney(BigDecimal value) {
        if (value == null) {
            return zeroMoney();
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    static Map<String, Object> buildDefaultNhanhOrderData(RequestContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestCode", context.requestCode());
        data.put("customerPhone", context.customerPhone());
        data.put("type", context.type() == null ? null : context.type().name());
        data.put("status", context.status() == null ? null : context.status().name());
        data.put("totalAmount", context.totalAmount());
        data.put("depositAmount", context.depositAmount());
        data.put("itemCount", context.itemCount());
        return data;
    }

    record RequestContext(
            String requestCode,
            String customerPhone,
            com.vn.sodu.request.OrderType type,
            com.vn.sodu.request.RequestStatus status,
            BigDecimal totalAmount,
            BigDecimal depositAmount,
            int itemCount
    ) {
    }
}
