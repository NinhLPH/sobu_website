package com.vn.sodu.user.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StatusChangeResponse {
    private Long id;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private LocalDateTime changedAt;
}
