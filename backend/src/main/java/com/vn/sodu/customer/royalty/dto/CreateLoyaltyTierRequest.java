package com.vn.sodu.customer.royalty.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreateLoyaltyTierRequest {
    private Double minTotalMoney;
    private Double discountRate;
    private String tierName;
}
