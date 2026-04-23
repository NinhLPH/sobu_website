package com.vn.sodu.customer.royalty.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class LoyaltyTransactionDTO {
    private Long id;
    private Long customerId;
    private String type;
    private Integer points;
    private String source;
    private Long referenceId;
    private String note;
    private LocalDateTime createdAt;
}
