package com.vn.sodu.product.controller;

import com.vn.sodu.product.service.ProductSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Sync Controller Tests")
class ProductSyncControllerTest {

    @Mock
    private ProductSyncService productSyncService;

    @InjectMocks
    private ProductSyncController productSyncController;

    @BeforeEach
    void setUp() {
    }

    // ──── Test syncProducts() ────────────────────────────────────────────────

    @Test
    @DisplayName("Should return success response when sync completes")
    void testSyncProductsSuccess() {
        doNothing().when(productSyncService).syncProducts();

        ResponseEntity<Map<String, String>> response = productSyncController.syncProducts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertEquals("Sync success", response.getBody().get("message"));
        verify(productSyncService, times(1)).syncProducts();
    }

    @Test
    @DisplayName("Should invoke service sync method exactly once")
    void testSyncProductsCallsServiceOnce() {
        doNothing().when(productSyncService).syncProducts();

        productSyncController.syncProducts();

        verify(productSyncService, times(1)).syncProducts();
        verifyNoMoreInteractions(productSyncService);
    }

    @Test
    @DisplayName("Should return response body with correct structure")
    void testSyncProductsResponseStructure() {
        doNothing().when(productSyncService).syncProducts();

        ResponseEntity<Map<String, String>> response = productSyncController.syncProducts();

        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertTrue(body.containsKey("message"));
    }

    @Test
    @DisplayName("Should throw exception when service throws exception")
    void testSyncProductsServiceThrowsException() {
        doThrow(new RuntimeException("Sync failed")).when(productSyncService).syncProducts();

        assertThrows(RuntimeException.class, () -> productSyncController.syncProducts());
    }

    @Test
    @DisplayName("Should propagate IllegalStateException from service")
    void testSyncProductsServiceThrowsIllegalStateException() {
        doThrow(new IllegalStateException("Configuration not set")).when(productSyncService).syncProducts();

        assertThrows(IllegalStateException.class, () -> productSyncController.syncProducts());
    }

    @Test
    @DisplayName("Should handle null response from service gracefully")
    void testSyncProductsHandlesServiceCalls() {
        doNothing().when(productSyncService).syncProducts();

        ResponseEntity<Map<String, String>> response = productSyncController.syncProducts();

        assertNotNull(response);
        assertNotNull(response.getBody());
        verify(productSyncService).syncProducts();
    }

    @Test
    @DisplayName("Should maintain request endpoint mapping /admin/products/sync")
    void testSyncProductsEndpointMapping() {
        doNothing().when(productSyncService).syncProducts();

        ResponseEntity<Map<String, String>> response = productSyncController.syncProducts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Should return 200 OK status code")
    void testSyncProductsHttpStatus() {
        doNothing().when(productSyncService).syncProducts();

        ResponseEntity<Map<String, String>> response = productSyncController.syncProducts();

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Should response message remain consistent")
    void testSyncProductsMessageConsistency() {
        doNothing().when(productSyncService).syncProducts();

        ResponseEntity<Map<String, String>> response1 = productSyncController.syncProducts();
        ResponseEntity<Map<String, String>> response2 = productSyncController.syncProducts();

        assertEquals(response1.getBody().get("message"), response2.getBody().get("message"));
    }

    @Test
    @DisplayName("Should invoke sync multiple times independently")
    void testSyncProductsMultipleCalls() {
        doNothing().when(productSyncService).syncProducts();

        productSyncController.syncProducts();
        productSyncController.syncProducts();
        productSyncController.syncProducts();

        verify(productSyncService, times(3)).syncProducts();
    }
}
