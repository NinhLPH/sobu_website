package com.vn.sodu.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class LoginRequest {
    private String email;
    private String password;
    // Future: support for OAuth2 provider
    // private String oauthProvider;
    // private String oauthToken;
}
