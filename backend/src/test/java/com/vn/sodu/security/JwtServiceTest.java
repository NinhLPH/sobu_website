package com.vn.sodu.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Service Tests")
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private UserDetails testUserDetails;
    private UserDetails anotherUserDetails;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "ThisIsAVeryLongSecretKeyThatIsAtLeast256BitsForHS256Algorithm");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 86400000L);

        testUserDetails = new User("testuser@example.com", "password", new HashSet<>());
        anotherUserDetails = new User("anotheruser@example.com", "password", new HashSet<>());
    }

    // ─── Generate Token Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should generate access token successfully")
    void testGenerateAccessToken() {
        String token = jwtService.generateAccessToken(testUserDetails);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("Should generate refresh token successfully")
    void testGenerateRefreshToken() {
        String token = jwtService.generateRefreshToken(testUserDetails);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("Generated access tokens should be different for different users")
    void testAccessTokensDifferentForDifferentUsers() {
        String token1 = jwtService.generateAccessToken(testUserDetails);
        String token2 = jwtService.generateAccessToken(anotherUserDetails);
        
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Generated refresh tokens should be different for different users")
    void testRefreshTokensDifferentForDifferentUsers() {
        String token1 = jwtService.generateRefreshToken(testUserDetails);
        String token2 = jwtService.generateRefreshToken(anotherUserDetails);
        
        assertNotEquals(token1, token2);
    }

    // ─── Extract Username Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should extract username from access token")
    void testExtractUsernameFromAccessToken() {
        String token = jwtService.generateAccessToken(testUserDetails);
        String username = jwtService.extractUsername(token);
        
        assertEquals("testuser@example.com", username);
    }

    @Test
    @DisplayName("Should extract username from refresh token")
    void testExtractUsernameFromRefreshToken() {
        String token = jwtService.generateRefreshToken(testUserDetails);
        String username = jwtService.extractUsername(token);
        
        assertEquals("testuser@example.com", username);
    }

    @Test
    @DisplayName("Should extract correct username for different user")
    void testExtractUsernameForDifferentUser() {
        String token = jwtService.generateAccessToken(anotherUserDetails);
        String username = jwtService.extractUsername(token);
        
        assertEquals("anotheruser@example.com", username);
    }

    // ─── Extract Expiration Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should extract expiration date from token")
    void testExtractExpirationDate() {
        String token = jwtService.generateAccessToken(testUserDetails);
        Date expiration = jwtService.extractExpiration(token);
        
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()), "Expiration date should be in the future");
    }

    @Test
    @DisplayName("Refresh token should have later expiration than access token")
    void testRefreshTokenHasLongerExpiration() {
        String accessToken = jwtService.generateAccessToken(testUserDetails);
        String refreshToken = jwtService.generateRefreshToken(testUserDetails);
        
        Date accessExpiration = jwtService.extractExpiration(accessToken);
        Date refreshExpiration = jwtService.extractExpiration(refreshToken);
        
        assertTrue(refreshExpiration.after(accessExpiration));
    }

    // ─── Token Validation Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should validate correct token with matching user")
    void testValidTokenWithMatchingUser() {
        String token = jwtService.generateAccessToken(testUserDetails);
        
        assertTrue(jwtService.isAccessTokenValid(token, testUserDetails));
    }

    @Test
    @DisplayName("Should reject token with non-matching user")
    void testInvalidTokenWithDifferentUser() {
        String token = jwtService.generateAccessToken(testUserDetails);
        
        assertFalse(jwtService.isAccessTokenValid(token, anotherUserDetails));
    }

    @Test
    @DisplayName("Should validate token without UserDetails")
    void testValidateTokenWithoutUserDetails() {
        String token = jwtService.generateAccessToken(testUserDetails);
        
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testRejectMalformedToken() {
        String malformedToken = "invalid.token.here";
        
        assertFalse(jwtService.isTokenValid(malformedToken));
    }

    @Test
    @DisplayName("Should reject token with wrong signature")
    void testRejectWrongSignature() {
        String validToken = jwtService.generateAccessToken(testUserDetails);
        String wrongToken = validToken.substring(0, validToken.length() - 1) + "x";
        
        assertFalse(jwtService.isTokenValid(wrongToken));
    }

    @Test
    @DisplayName("Should reject refresh token for access-token validation")
    void testAccessValidationRejectsRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken(testUserDetails);

        assertFalse(jwtService.isAccessTokenValid(refreshToken, testUserDetails));
    }

    @Test
    @DisplayName("Should validate refresh token only for refresh-token validation")
    void testRefreshValidationRequiresRefreshToken() {
        String accessToken = jwtService.generateAccessToken(testUserDetails);
        String refreshToken = jwtService.generateRefreshToken(testUserDetails);

        assertTrue(jwtService.isRefreshTokenValid(refreshToken));
        assertFalse(jwtService.isRefreshTokenValid(accessToken));
    }

    // ─── Token Expiration Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should not consider fresh token as expired")
    void testFreshTokenNotExpired() {
        String token = jwtService.generateAccessToken(testUserDetails);
        
        assertFalse(jwtService.isTokenExpired(token));
    }

    // ─── Extract Claim Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should extract custom claim from token")
    void testExtractCustomClaim() {
        String refreshToken = jwtService.generateRefreshToken(testUserDetails);
        String tokenType = jwtService.extractTokenType(refreshToken);
        
        assertEquals("refresh", tokenType);
    }

    @Test
    @DisplayName("Should mark access tokens with access type claim")
    void testAccessTokenTypeClaim() {
        String accessToken = jwtService.generateAccessToken(testUserDetails);

        assertEquals("access", jwtService.extractTokenType(accessToken));
    }

    @Test
    @DisplayName("Should expose access token expiration in seconds")
    void testAccessTokenExpiresInSeconds() {
        assertEquals(3600L, jwtService.getAccessTokenExpiresInSeconds());
    }

    @Test
    @DisplayName("Should extract subject (username) claim")
    void testExtractSubjectClaim() {
        String token = jwtService.generateAccessToken(testUserDetails);
        String subject = jwtService.extractClaim(token, claims -> claims.getSubject());
        
        assertEquals("testuser@example.com", subject);
    }

    @Test
    @DisplayName("Should extract issued-at claim")
    void testExtractIssuedAtClaim() {
        String token = jwtService.generateAccessToken(testUserDetails);
        Date issuedAt = jwtService.extractClaim(token, claims -> claims.getIssuedAt());
        
        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new Date()) || issuedAt.equals(new Date()));
    }

    // ─── Edge Cases ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should handle empty token")
    void testHandleEmptyToken() {
        assertThrows(Exception.class, () -> jwtService.extractUsername(""));
    }

    @Test
    @DisplayName("Should handle null token")
    void testHandleNullToken() {
        assertThrows(Exception.class, () -> jwtService.extractUsername(null));
    }

    @Test
    @DisplayName("Should generate multiple tokens without issues")
    void testGenerateMultipleTokens() {
        for (int i = 0; i < 10; i++) {
            String token = jwtService.generateAccessToken(testUserDetails);
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertEquals("testuser@example.com", jwtService.extractUsername(token));
        }
    }
}
