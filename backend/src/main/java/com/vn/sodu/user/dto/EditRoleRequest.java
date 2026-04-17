package com.vn.sodu.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EditRoleRequest {
    private String name;
    private String description;
}
