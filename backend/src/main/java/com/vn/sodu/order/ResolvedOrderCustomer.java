package com.vn.sodu.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResolvedOrderCustomer {
    private String fullName;
    private String email;
    private String phone;
    private String street;
    private String province;
    private String district;
    private String ward;
}
