package com.vn.sodu.global.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String message;
    private String error;
    private int statusCode;
    private String path;
    private String timestamp;

    public static ErrorResponse of(String message, int statusCode) {
        return ErrorResponse.builder()
                .message(message)
                .error(message)
                .statusCode(statusCode)
                .build();
    }

    public static ErrorResponse of(String message, String error, int statusCode) {
        return ErrorResponse.builder()
                .message(message)
                .error(error)
                .statusCode(statusCode)
                .build();
    }
}
