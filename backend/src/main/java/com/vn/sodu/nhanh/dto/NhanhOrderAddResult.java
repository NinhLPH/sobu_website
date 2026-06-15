package com.vn.sodu.nhanh.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhanhOrderAddResult {
    private Long id;
    @JsonAlias({"orderId", "order_id", "saleOrderId", "sale_order_id"})
    private Long orderId;
    private String trackingUrl;
    private boolean duplicate;
    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    public static NhanhOrderAddResult duplicate() {
        NhanhOrderAddResult result = new NhanhOrderAddResult();
        result.setDuplicate(true);
        return result;
    }

    @JsonAnySetter
    public void putAdditionalProperty(String key, Object value) {
        additionalProperties.put(key, value);
    }

    public String resolveNhanhOrderId() {
        if (id != null) {
            return id.toString();
        }
        if (orderId != null) {
            return orderId.toString();
        }

        String nestedOrderId = resolveKnownOrderId(additionalProperties);
        if (nestedOrderId != null) {
            return nestedOrderId;
        }

        return extractTrailingDigits(trackingUrl);
    }

    private String resolveKnownOrderId(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        for (String key : List.of("id", "orderId", "order_id", "saleOrderId", "sale_order_id")) {
            String candidate = numericString(values.get(key));
            if (candidate != null) {
                return candidate;
            }
        }

        for (String key : List.of("order", "saleOrder", "sale_order", "data", "result")) {
            Object nested = values.get(key);
            if (nested instanceof Map<?, ?> nestedMap) {
                @SuppressWarnings("unchecked")
                String candidate = resolveKnownOrderId((Map<String, Object>) nestedMap);
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private String numericString(Object value) {
        if (value instanceof Number number) {
            return Long.toString(number.longValue());
        }
        if (!(value instanceof String text)) {
            return null;
        }
        String trimmed = text.trim();
        if (!trimmed.matches("\\d+")) {
            return null;
        }
        return trimmed;
    }

    private String extractTrailingDigits(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        int end = trimmed.length() - 1;
        while (end >= 0 && !Character.isDigit(trimmed.charAt(end))) {
            end--;
        }
        if (end < 0) {
            return null;
        }
        int start = end;
        while (start >= 0 && Character.isDigit(trimmed.charAt(start))) {
            start--;
        }
        return trimmed.substring(start + 1, end + 1);
    }
}
