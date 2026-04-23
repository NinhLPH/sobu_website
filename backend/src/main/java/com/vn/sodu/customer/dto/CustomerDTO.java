package com.vn.sodu.customer.dto;

import com.vn.sodu.customer.royalty.dto.LoyaltyTierDTO;
import com.vn.sodu.user.dto.AccountDTO;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CustomerDTO {
    private Long id;
    private Integer gender;
    private LocalDate birthday;
    private String province;
    private String district;
    private String ward;
    private String street;
    private Double totalMoney;
    private Integer points;
    private LoyaltyTierDTO tier;
    private AccountDTO account;
}
