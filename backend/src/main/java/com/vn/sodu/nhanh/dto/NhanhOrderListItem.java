package com.vn.sodu.nhanh.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhanhOrderListItem {

    private Info info;
    private Channel channel;
    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    @JsonAnySetter
    public void putAdditionalProperty(String key, Object value) {
        additionalProperties.put(key, value);
    }

    public String resolveNhanhOrderId() {
        if (info != null && info.getId() != null) {
            return info.getId().toString();
        }
        return resolveKnownValue(additionalProperties, List.of("id", "orderId", "order_id"));
    }

    public boolean matchesReference(String appOrderId, String orderCode) {
        String normalizedAppOrderId = normalize(appOrderId);
        String normalizedOrderCode = normalize(orderCode);
        String channelAppOrderId = channel == null ? null : normalize(channel.getAppOrderId());
        String extractedOrderCode = normalize(resolveOrderCode());

        return Objects.equals(channelAppOrderId, normalizedAppOrderId)
                || Objects.equals(channelAppOrderId, normalizedOrderCode)
                || Objects.equals(extractedOrderCode, normalizedOrderCode)
                || Objects.equals(extractedOrderCode, normalizedAppOrderId);
    }

    public String resolveOrderCode() {
        if (info != null) {
            String fromInfo = info.resolveOrderCode();
            if (fromInfo != null) {
                return fromInfo;
            }
        }
        return resolveKnownValue(additionalProperties, List.of("code", "orderCode", "order_code"));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String resolveKnownValue(Map<String, Object> values, List<String> keys) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        for (String key : keys) {
            String candidate = textValue(values.get(key));
            if (candidate != null) {
                return candidate;
            }
        }

        for (String key : List.of("info", "channel", "order", "data", "result")) {
            Object nested = values.get(key);
            if (nested instanceof Map<?, ?> nestedMap) {
                @SuppressWarnings("unchecked")
                String candidate = resolveKnownValue((Map<String, Object>) nestedMap, keys);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private Long id;
        private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

        @JsonAnySetter
        public void putAdditionalProperty(String key, Object value) {
            additionalProperties.put(key, value);
        }

        public String resolveOrderCode() {
            for (String key : List.of("code", "orderCode", "order_code")) {
                Object value = additionalProperties.get(key);
                if (value != null) {
                    String text = value.toString().trim();
                    if (!text.isEmpty()) {
                        return text;
                    }
                }
            }
            return null;
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {
        @JsonAlias({"appOrderId", "app_order_id"})
        private String appOrderId;
    }
}
