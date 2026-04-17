package com.vn.sodu.customer.royalty.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class LoyaltyTransactionResponse {
    private Long id;
    private Long customerId;
    private String type;
    private Integer points;
    private String source;
    private Long referenceId;
    private String note;
    private String createdAt;
    private String customerEmail;
    private String customerName;
}
