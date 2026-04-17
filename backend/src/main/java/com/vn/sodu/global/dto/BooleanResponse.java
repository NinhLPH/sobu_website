package com.vn.sodu.global.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class BooleanResponse {
    private boolean success;
    private String message;

    public static BooleanResponse success() {
        return BooleanResponse.builder()
                .success(true)
                .build();
    }

    public static BooleanResponse success(String message) {
        return BooleanResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static BooleanResponse failure() {
        return BooleanResponse.builder()
                .success(false)
                .build();
    }

    public static BooleanResponse failure(String message) {
        return BooleanResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
