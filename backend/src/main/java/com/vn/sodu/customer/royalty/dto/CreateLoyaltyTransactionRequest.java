package com.vn.sodu.customer.royalty.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreateLoyaltyTransactionRequest {
    private Long customerId;
    private String type; // EARN, REDEEM, ADJUST
    private Integer points;
    private String source; // ORDER, ADMIN, PROMOTION
    private Long referenceId;
    private String note;
}
