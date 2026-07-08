package com.vn.sodu.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.user.dto.AccountDTO;
import com.vn.sodu.user.dto.LoginRequest;
import com.vn.sodu.user.dto.LoginResponse;
import com.vn.sodu.user.dto.RefreshTokenRequest;
import com.vn.sodu.user.dto.UpdatePhoneRequest;
import com.vn.sodu.global.exception.UnauthorizedException;
import com.vn.sodu.user.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.vn.sodu.security.JwtService jwtService;

    @MockBean
    private com.vn.sodu.security.TokenBlacklistService tokenBlacklistService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/login - success")
    void testLoginSuccess() throws Exception {
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

        mockMvc.perform(post("/api/auth/login").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    @DisplayName("POST /auth/login - compatibility alias success")
    void testLoginAliasSuccess() throws Exception {
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

        mockMvc.perform(post("/auth/login").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    @DisplayName("POST /api/auth/login - failure returns 401")
    void testLoginFailure() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("bad@example.com");
        req.setPassword("bad");

        Mockito.when(authService.login(any(LoginRequest.class))).thenThrow(new UnauthorizedException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/refresh-token - success")
    void testRefreshTokenSuccess() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("validRefreshToken");

        LoginResponse resp = LoginResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("validRefreshToken")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        Mockito.when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/refresh-token").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    @DisplayName("POST /api/auth/refresh-token - invalid token returns 401")
    void testRefreshTokenFailure() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("invalid");

        Mockito.when(authService.refreshToken(any(RefreshTokenRequest.class))).thenThrow(new UnauthorizedException("Invalid or expired refresh token"));

        mockMvc.perform(post("/api/auth/refresh-token").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/logout - success forwards access and refresh tokens")
    void testLogoutSuccess() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(eq("access-token"), eq("refresh-token"));
    }

    @Test
    @DisplayName("PATCH /api/auth/me/phone - success")
    void testUpdateCurrentAccountPhoneSuccess() throws Exception {
        UpdatePhoneRequest req = new UpdatePhoneRequest();
        req.setPhone("0987654321");

        AccountDTO account = AccountDTO.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .phone("0987654321")
                .status("ACTIVE")
                .build();

        Mockito.when(authService.updateCurrentAccountPhone(any(UpdatePhoneRequest.class))).thenReturn(account);

        mockMvc.perform(patch("/api/auth/me/phone")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phone").value("0987654321"));

        verify(authService).updateCurrentAccountPhone(argThat(request -> "0987654321".equals(request.getPhone())));
    }
}
