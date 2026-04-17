package com.vn.sodu.global.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StatusChangeResponse {
    private String status;
    private String message;

    public static StatusChangeResponse of(String status, String message) {
        return StatusChangeResponse.builder()
                .status(status)
                .message(message)
                .build();
    }
}
