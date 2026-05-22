package com.vn.sodu.product.service;

import com.vn.sodu.nhanh.NhanhIntegration;
import com.vn.sodu.nhanh.service.NhanhClient;
import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.repo.ProductAttributeRepo;
import com.vn.sodu.product.repo.ProductImageRepo;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.repo.ProductUnitRepo;
import com.vn.sodu.product.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Sync Service Incremental Tests")
class ProductSyncServiceIncrementalTest {

    @Mock
    private ProductRepo productRepo;
    @Mock
    private ProductUnitRepo productUnitRepo;
    @Mock
    private ProductAttributeRepo productAttributeRepo;
    @Mock
    private ProductImageRepo productImageRepo;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private NhanhService nhanhService;
    @Mock
    private NhanhClient nhanhClient;

    @InjectMocks
    private ProductSyncService productSyncService;



    @Test
    @DisplayName("Should use lastSyncTime from integration and update it after sync")
    void testIncrementalSyncFlow() {
        // Setup
        NhanhIntegration integration = new NhanhIntegration();
        integration.setLastProductSyncTime(1000L);
        when(nhanhService.getIntegration()).thenReturn(Optional.of(integration));

        // Let's use a spy to mock fetchAllProducts
        ProductSyncService spyService = spy(productSyncService);
        doReturn(Collections.emptyList()).when(spyService).fetchAllProducts(anyLong());

        spyService.syncProducts();

        verify(spyService).fetchAllProducts(eq(1000L));
        verify(nhanhService).updateLastSyncTime(anyLong());
        
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(nhanhService).updateLastSyncTime(timeCaptor.capture());
        assertTrue(timeCaptor.getValue() > 1000L);
    }

    @Test
    @DisplayName("Should handle missing integration (full sync)")
    void testSyncWithoutIntegration() {
        when(nhanhService.getIntegration()).thenReturn(Optional.empty());
        
        ProductSyncService spyService = spy(productSyncService);
        doReturn(Collections.emptyList()).when(spyService).fetchAllProducts(null);

        spyService.syncProducts();

        verify(spyService).fetchAllProducts(null);
        verify(nhanhService).updateLastSyncTime(anyLong());
    }

    @Test
    @DisplayName("Should not advance last sync time when any product fails")
    void testDoesNotAdvanceCursorWhenProductFails() {
        NhanhProductDTO dto = new NhanhProductDTO();
        dto.setId(123L);
        when(nhanhService.getIntegration()).thenReturn(Optional.empty());

        ProductSyncService spyService = spy(productSyncService);
        doReturn(Collections.singletonList(dto)).when(spyService).fetchAllProducts(null);

        assertThrows(IllegalStateException.class, spyService::syncProducts);
        verify(nhanhService, never()).updateLastSyncTime(anyLong());
    }

}
