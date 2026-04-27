package com.vn.sodu.product.service;

import com.vn.sodu.nhanh.NhanhIntegration;
import com.vn.sodu.nhanh.service.NhanhService;
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

    @InjectMocks
    private ProductSyncService productSyncService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productSyncService, "nhanhBaseUrl", "https://pos.open.nhanh.vn/api");
        ReflectionTestUtils.setField(productSyncService, "clientId", "77323");
        ReflectionTestUtils.setField(productSyncService, "businessId", "224003");
    }

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
    @DisplayName("Should build incremental request body using filters and paginator")
    void testIncrementalRequestBodyShape() {
        Long timestamp = 1704067200L;

        Map<String, Object> body = ReflectionTestUtils.invokeMethod(
                productSyncService, "buildProductListRequestBody", timestamp, null);

        assertNotNull(body);
        assertTrue(body.containsKey("filters"));
        assertTrue(body.containsKey("paginator"));
        assertFalse(body.containsKey("page"));
        assertFalse(body.containsKey("pageSize"));

        Map<String, Object> filters = (Map<String, Object>) body.get("filters");
        Map<String, Object> paginator = (Map<String, Object>) body.get("paginator");
        assertEquals(timestamp, filters.get("updatedAtFrom"));
        assertEquals(50, paginator.get("size"));
        assertFalse(paginator.containsKey("next"));
    }

    @Test
    @DisplayName("Should build product URL from base URL, client id, and business id")
    void testBuildProductListUrl() {
        String url = ReflectionTestUtils.invokeMethod(productSyncService, "buildProductListUrl");

        assertEquals("https://pos.open.nhanh.vn/v3.0/product/list?appId=77323&businessId=224003", url);
    }

    @Test
    @DisplayName("Should omit updatedAtFrom when last sync time is zero")
    void testFullSyncRequestBodyWhenLastSyncIsZero() {
        Map<String, Object> body = ReflectionTestUtils.invokeMethod(
                productSyncService, "buildProductListRequestBody", 0L, null);

        assertNotNull(body);
        assertFalse(body.containsKey("filters"));
        assertTrue(body.containsKey("paginator"));

        Map<String, Object> paginator = (Map<String, Object>) body.get("paginator");
        assertEquals(50, paginator.get("size"));
        assertFalse(paginator.containsKey("next"));
    }
}
