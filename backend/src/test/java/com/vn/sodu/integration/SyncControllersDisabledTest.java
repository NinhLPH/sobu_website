package com.vn.sodu.integration;

import com.vn.sodu.product.brand.controller.BrandSyncController;
import com.vn.sodu.product.brand.service.BrandSyncService;
import com.vn.sodu.product.category.controller.CategorySyncController;
import com.vn.sodu.product.category.service.CategorySyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SyncControllersDisabledTest {

    @Mock
    private ObjectProvider<CategorySyncService> categorySyncServiceProvider;

    @Mock
    private ObjectProvider<BrandSyncService> brandSyncServiceProvider;

    @Mock
    private NhanhEnabled nhanhEnabled;

    @BeforeEach
    void rejectNhanhCalls() {
        doThrow(new NhanhIntegrationDisabledException()).when(nhanhEnabled).requireEnabled();
    }

    @Test
    void categorySyncRejectsLocalModeBeforeResolvingService() {
        CategorySyncController controller = new CategorySyncController(
                categorySyncServiceProvider,
                nhanhEnabled
        );

        assertThrows(NhanhIntegrationDisabledException.class, controller::syncCategories);
        verify(categorySyncServiceProvider, never()).getIfAvailable();
    }

    @Test
    void brandSyncRejectsLocalModeBeforeResolvingService() {
        BrandSyncController controller = new BrandSyncController(
                brandSyncServiceProvider,
                nhanhEnabled
        );

        assertThrows(NhanhIntegrationDisabledException.class, controller::syncBrands);
        verify(brandSyncServiceProvider, never()).getIfAvailable();
    }
}
