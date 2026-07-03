package com.vn.sodu.support.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vn.sodu.support.ConversationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationSummaryDTO {
    private Long id;
    private ConversationStatus status;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private String customerEmail;
    private String customerName;
}
