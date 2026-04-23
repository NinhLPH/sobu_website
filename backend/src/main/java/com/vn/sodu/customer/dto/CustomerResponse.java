package com.vn.sodu.customer.dto;

import com.vn.sodu.customer.royalty.dto.LoyaltyTierDTO;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CustomerResponse {
    private Long id;
    private Integer gender;
    private String birthday;
    private String province;
    private String district;
    private String ward;
    private String street;
    private Double totalMoney;
    private Integer points;
    private LoyaltyTierDTO tier;
    private String email;
    private String fullName;
    private String phone;
}
