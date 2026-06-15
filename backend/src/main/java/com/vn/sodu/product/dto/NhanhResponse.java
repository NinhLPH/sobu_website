package com.vn.sodu.product.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NhanhResponse<T> {
    private int code;
    private T data;
    @JsonDeserialize(using = MessagesDeserializer.class)
    private List<String> messages;

    private Paginator paginator;

    public NhanhResponse(int code, T data, Paginator paginator) {
        this.code = code;
        this.data = data;
        this.paginator = paginator;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paginator {
        private Object next;
    }

    static class MessagesDeserializer extends JsonDeserializer<List<String>> {
        @Override
        public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            List<String> messages = new ArrayList<>();
            append(messages, node);
            return messages;
        }

        private void append(List<String> messages, JsonNode node) {
            if (node == null || node.isNull() || node.isMissingNode()) {
                return;
            }
            if (node.isArray()) {
                node.forEach(item -> append(messages, item));
                return;
            }

            String message = flatten(node);
            if (message != null && !message.isBlank()) {
                messages.add(message);
            }
        }

        private String flatten(JsonNode node) {
            if (node == null || node.isNull() || node.isMissingNode()) {
                return null;
            }
            if (node.isTextual() || node.isNumber() || node.isBoolean()) {
                return node.asText();
            }
            if (!node.isObject()) {
                return node.toString();
            }

            String code = text(node, "code", "errorCode");
            String message = text(node, "message", "msg", "error", "description", "detail");
            if (message != null && code != null && !message.contains(code)) {
                return code + ": " + message;
            }
            if (message != null) {
                return message;
            }
            if (code != null) {
                return code;
            }
            return node.toString();
        }

        private String text(JsonNode node, String... fieldNames) {
            for (String fieldName : fieldNames) {
                JsonNode child = node.get(fieldName);
                if (child != null && !child.isNull()) {
                    String value = child.asText();
                    if (value != null && !value.isBlank()) {
                        return value;
                    }
                }
            }
            return null;
        }
    }
}
