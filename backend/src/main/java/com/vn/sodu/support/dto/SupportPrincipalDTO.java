package com.vn.sodu.support.dto;

import lombok.Builder;

@Builder
public record SupportPrincipalDTO(
        Long accountId,
        String email,
        String roleName,
        boolean staff,
        boolean active
) {
}
