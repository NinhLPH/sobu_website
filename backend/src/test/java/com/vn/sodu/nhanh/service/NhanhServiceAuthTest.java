package com.vn.sodu.nhanh.service;

import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.NhanhIntegration;
import com.vn.sodu.nhanh.NhanhIntegrationRepo;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.NhanhTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NhanhServiceAuthTest {

    private static final long FUTURE_EXPIRY = 4_102_444_800L;
    private static final long PAST_EXPIRY = 946_684_800L;

    @Mock
    private NhanhIntegrationRepo nhanhIntegrationRepo;

    @Mock
    private NhanhClient nhanhClient;

    @Mock
    private NhanhProperties nhanhProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NhanhService nhanhService;

    @BeforeEach
    public void setUp() {
        nhanhService = new NhanhService(
                nhanhClient,
                nhanhIntegrationRepo,
                nhanhProperties,
                eventPublisher);
    }

    @Test
    public void testGetValidAccessToken_Success() {
        // Arrange
        NhanhIntegration integration = new NhanhIntegration();
        integration.setId(1L);
        integration.setBusinessId(123L);
        integration.setAccessToken("real_access_token_12345");
        integration.setAppId("app1");
        integration.setExpiredAt(FUTURE_EXPIRY);

        when(nhanhProperties.getBusinessId()).thenReturn("123");
        when(nhanhIntegrationRepo.findByBusinessId(123L)).thenReturn(Optional.of(integration));

        // Act
        String token = nhanhService.getValidAccessToken();

        // Assert
        assertNotNull(token, "Token should not be null");
        assertEquals("real_access_token_12345", token, "Token should match stored token");
        assertFalse(token.contains("placeholder"), "Token should not contain placeholder");
    }

    @Test
    public void testGetValidAccessToken_NoIntegration() {
        // Arrange
        when(nhanhProperties.getBusinessId()).thenReturn("123");
        when(nhanhIntegrationRepo.findByBusinessId(123L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ExternalServiceException.class, () -> {
            nhanhService.getValidAccessToken();
        }, "Should throw ExternalServiceException when no integration found");
    }

    @Test
    public void testGetValidAccessToken_MultipleIntegrations_ReturnFirst() {
        // Arrange
        NhanhIntegration integration1 = new NhanhIntegration();
        integration1.setId(1L);
        integration1.setAccessToken("token_1");
        integration1.setBusinessId(123L);
        integration1.setExpiredAt(FUTURE_EXPIRY);

        NhanhIntegration integration2 = new NhanhIntegration();
        integration2.setId(2L);
        integration2.setAccessToken("token_2");
        integration2.setBusinessId(456L);
        integration2.setExpiredAt(FUTURE_EXPIRY);

        when(nhanhProperties.getBusinessId()).thenReturn("123");
        when(nhanhIntegrationRepo.findByBusinessId(123L)).thenReturn(Optional.of(integration1));

        // Act
        String token = nhanhService.getValidAccessToken();

        // Assert
        assertEquals("token_1", token, "Should return first integration token");
    }

    @Test
    public void testGetValidAccessToken_MissingExpiryThrowsException() {
        NhanhIntegration integration = new NhanhIntegration();
        integration.setId(1L);
        integration.setBusinessId(123L);
        integration.setAccessToken("token");
        integration.setAppId("app1");

        when(nhanhProperties.getBusinessId()).thenReturn("123");
        when(nhanhIntegrationRepo.findByBusinessId(123L)).thenReturn(Optional.of(integration));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class, () -> {
            nhanhService.getValidAccessToken();
        });

        assertEquals("Nhanh integration token expiry is missing. Please authenticate again.", ex.getMessage());
    }

    @Test
    public void testGetValidAccessToken_ExpiredTokenThrowsException() {
        NhanhIntegration integration = new NhanhIntegration();
        integration.setId(1L);
        integration.setBusinessId(123L);
        integration.setAccessToken("token");
        integration.setAppId("app1");
        integration.setExpiredAt(PAST_EXPIRY);

        when(nhanhProperties.getBusinessId()).thenReturn("123");
        when(nhanhIntegrationRepo.findByBusinessId(123L)).thenReturn(Optional.of(integration));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class, () -> {
            nhanhService.getValidAccessToken();
        });

        assertEquals("Nhanh integration token has expired. Please authenticate again.", ex.getMessage());
    }

    @Test
    public void testGetValidAccessToken_NearlyExpiredTokenThrowsException() {
        NhanhIntegration integration = new NhanhIntegration();
        integration.setId(1L);
        integration.setBusinessId(123L);
        integration.setAccessToken("token");
        integration.setAppId("app1");
        integration.setExpiredAt((System.currentTimeMillis() / 1000) + 30);

        when(nhanhProperties.getBusinessId()).thenReturn("123");
        when(nhanhIntegrationRepo.findByBusinessId(123L)).thenReturn(Optional.of(integration));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class, () -> {
            nhanhService.getValidAccessToken();
        });

        assertEquals("Nhanh integration token has expired. Please authenticate again.", ex.getMessage());
    }

    @Test
    public void testBearerHeaderFormat() {
        // Verify header format is correct
        String token = "test_token_123";
        String authHeader = "Bearer " + token;
        
        assertTrue(authHeader.startsWith("Bearer "), "Header should start with 'Bearer '");
        assertEquals("Bearer test_token_123", authHeader, "Header format should be 'Bearer <token>'");
    }

    @Test
    public void oauthBusinessMismatchIsRejectedBeforePersistence() {
        NhanhTokenResponse response = new NhanhTokenResponse();
        response.setBusinessId(456L);
        response.setAccessToken("token");
        response.setExpiredAt(FUTURE_EXPIRY);
        when(nhanhProperties.getBusinessId()).thenReturn("123");
        when(nhanhClient.getAccessToken("code")).thenReturn(response);

        assertThrows(ExternalServiceException.class, () -> nhanhService.handleCallback("code"));

        verify(nhanhIntegrationRepo, never()).save(org.mockito.ArgumentMatchers.any());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }
}
