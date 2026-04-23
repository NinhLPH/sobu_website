package com.vn.sodu.customer.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UpdateCustomerRequest {
    private Integer gender;
    private LocalDate birthday;
    private String province;
    private String district;
    private String ward;
    private String street;
}
