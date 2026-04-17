package com.vn.sodu.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AccountResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private RoleDTO role;
    private String status;
}
