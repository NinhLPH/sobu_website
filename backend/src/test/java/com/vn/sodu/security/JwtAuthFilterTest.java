package com.vn.sodu.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Auth Filter Tests")
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUserDetails = new User("test@example.com", "password", Collections.emptyList());
    }

    // ─── Valid Token Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should set authentication with valid token")
    void testValidTokenSetsAuthentication() throws ServletException, IOException {
        String token = "validJwtToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtService.isAccessTokenValid(token, testUserDetails)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("test@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should include user authorities in authentication")
    void testAuthenticationIncludesAuthorities() throws ServletException, IOException {
        String token = "validJwtToken";
        UserDetails userWithAuthorities = new User("test@example.com", "password", 
            Collections.singletonList(() -> "ROLE_USER"));
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userWithAuthorities);
        when(jwtService.isAccessTokenValid(token, userWithAuthorities)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals(1, SecurityContextHolder.getContext().getAuthentication().getAuthorities().size());
        verify(filterChain).doFilter(request, response);
    }

    // ─── Missing Authorization Header Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should skip filter when authorization header is missing")
    void testMissingAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("Should skip filter when authorization header is empty")
    void testEmptyAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ─── Invalid Bearer Token Format Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should skip filter when authorization header doesn't start with Bearer")
    void testInvalidBearerFormat() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("Should skip filter when Bearer token is missing")
    void testMissingBearerToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ─── Invalid Token Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should skip authentication with invalid token")
    void testInvalidTokenSkipsAuthentication() throws ServletException, IOException {
        String token = "invalidToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtService.isAccessTokenValid(token, testUserDetails)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle exception during token extraction")
    void testExceptionDuringTokenExtraction() throws ServletException, IOException {
        String token = "malformedToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("Invalid token"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should reject refresh token for request authentication")
    void testRefreshTokenDoesNotAuthenticateRequest() throws ServletException, IOException {
        String token = "refreshToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtService.isAccessTokenValid(token, testUserDetails)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ─── User Details Loading Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should load user details from service")
    void testLoadUserDetailsFromService() throws ServletException, IOException {
        String token = "validToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtService.isAccessTokenValid(token, testUserDetails)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService).loadUserByUsername("test@example.com");
    }

    @Test
    @DisplayName("Should handle user not found scenario")
    void testUserNotFoundHandling() throws ServletException, IOException {
        String token = "validToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("nonexistent@example.com");
        when(userDetailsService.loadUserByUsername("nonexistent@example.com"))
            .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // ─── Multiple Filter Chain Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should continue filter chain regardless of authentication")
    void testFilterChainContinuesAfterValidation() throws ServletException, IOException {
        String token = "validToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtService.isAccessTokenValid(token, testUserDetails)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain even when token is invalid")
    void testFilterChainContinuesWithInvalidToken() throws ServletException, IOException {
        String token = "invalidToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtService.isAccessTokenValid(token, testUserDetails)).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ─── Security Context Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should not override existing authentication")
    void testDoesNotOverrideExistingAuthentication() throws ServletException, IOException {
        String token = "validToken";
        var existingAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "existing@example.com", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals("existing@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    @DisplayName("Should set authentication details from request")
    void testSetsAuthenticationDetailsFromRequest() throws ServletException, IOException {
        String token = "validToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtService.isAccessTokenValid(token, testUserDetails)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication().getDetails());
    }

    @Test
    @DisplayName("Should reject blacklisted token before authentication")
    void testBlacklistedTokenSkipsAuthentication() throws ServletException, IOException {
        String token = "blacklistedToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUsername(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should reject locked or disabled users even with valid token")
    void testLockedUserDoesNotAuthenticate() throws ServletException, IOException {
        String token = "validJwtToken";
        UserDetails lockedUser = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .accountLocked(true)
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(lockedUser);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).isAccessTokenValid(token, lockedUser);
        verify(filterChain).doFilter(request, response);
    }
}
