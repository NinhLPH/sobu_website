package com.vn.sodu.customer.royalty.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CreateLoyaltyRuleRequest {
    private String code;
    private String value;
    private Boolean active;
}
