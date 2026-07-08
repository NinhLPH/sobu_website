package com.vn.sodu.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePhoneRequest {
    private String phone;
}
