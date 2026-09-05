package com.vn.sodu.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.config.WebConfig;
import com.vn.sodu.security.JwtAuthFilter;
import com.vn.sodu.security.JwtService;
import com.vn.sodu.security.SecurityConfig;
import com.vn.sodu.security.TokenBlacklistService;
import com.vn.sodu.user.dto.*;
import com.vn.sodu.user.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, WebConfig.class})
@TestPropertySource(properties = "cors.allowed-origins=https://sobu-jet.vercel.app")
@DisplayName("Auth Controller Security Tests")
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.vn.sodu.user.service.OAuthService oAuthService;

    @MockBean
    private com.vn.sodu.user.service.PasswordResetService passwordResetService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /auth/login is public")
    void loginAliasIsPublic() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");

        LoginResponse resp = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password and /auth/forgot-password are public")
    void forgotPasswordIsPublic() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("user@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password and /auth/reset-password are public")
    void resetPasswordIsPublic() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest("valid-token", "newpassword123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Public API preflight allows Vercel frontend origin")
    void publicApiPreflightAllowsVercelOrigin() throws Exception {
        mockMvc.perform(options("/api/public/brands")
                        .header(HttpHeaders.ORIGIN, "https://sobu-jet.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://sobu-jet.vercel.app"));
    }

    @Test
    @DisplayName("API preflight allows PATCH method")
    void apiPreflightAllowsPatchMethod() throws Exception {
        mockMvc.perform(options("/api/admin/products/1/active")
                        .header(HttpHeaders.ORIGIN, "https://sobu-jet.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://sobu-jet.vercel.app"))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        org.hamcrest.Matchers.containsString("PATCH")));
    }
}
