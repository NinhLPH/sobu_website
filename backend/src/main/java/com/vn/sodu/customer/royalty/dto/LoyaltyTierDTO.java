package com.vn.sodu.customer.royalty.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class LoyaltyTierDTO {
    private Long id;
    private Double minTotalMoney;
    private Double discountRate;
    private String tierName;
}
