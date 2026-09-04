package com.vn.sodu.product.brand.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.nhanh.service.NhanhClient;
import com.vn.sodu.integration.NhanhEnabled;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandSyncServiceTest {

    @Mock
    private BrandRepo brandRepo;
    @Mock
    private BrandMapper brandMapper;
    @Mock
    private NhanhService nhanhService;
    @Mock
    private NhanhClient nhanhClient;

    @Mock
    private NhanhEnabled nhanhEnabled;

    @InjectMocks
    private BrandSyncService brandSyncService;

    @Test
    @DisplayName("Should skip sync when brand dto has null id")
    void testSyncOneSkipsNullId() {
        boolean synced = brandSyncService.syncOne(new NhanhBrandDTO(null, null, "B", "Brand", 1, 1L));

        assertFalse(synced);
        verifyNoInteractions(brandMapper, brandRepo);
    }

    @Test
    @DisplayName("Should skip sync when mapper returns null")
    void testSyncOneSkipsWhenMapperNull() {
        NhanhBrandDTO dto = new NhanhBrandDTO(1L, null, "B", "Brand", 1, 1L);
        when(brandMapper.toEntity(dto)).thenReturn(null);

        boolean synced = brandSyncService.syncOne(dto);

        assertFalse(synced);
        verify(brandMapper).toEntity(dto);
        verifyNoInteractions(brandRepo);
    }

    @Test
    @DisplayName("Should save mapped brand")
    void testSyncOneSavesBrand() {
        NhanhBrandDTO dto = new NhanhBrandDTO(1L, null, "B", "Brand", 1, 1L);
        Brand brand = Brand.builder().id(1L).name("Brand").build();
        when(brandMapper.toEntity(dto)).thenReturn(brand);

        boolean synced = brandSyncService.syncOne(dto);

        assertTrue(synced);
        verify(brandRepo).save(brand);
    }

    @Test
    @DisplayName("Should resolve parent external id to local primary key without remapping child")
    void testResolveParentUsesPersistedLocalEntities() {
        NhanhBrandDTO childDto = new NhanhBrandDTO(200L, 100L, "C", "Child", 1, 1L);
        Brand child = Brand.builder().id(42L).externalId(200L).name("Child").build();
        Brand parent = Brand.builder().id(17L).externalId(100L).name("Parent").build();
        when(brandRepo.findByExternalId(200L)).thenReturn(java.util.Optional.of(child));
        when(brandRepo.findByExternalId(100L)).thenReturn(java.util.Optional.of(parent));

        assertTrue(brandSyncService.resolveParent(childDto));

        verify(brandRepo).save(argThat(brand ->
                brand.getId().equals(42L) && brand.getParentId().equals(17L)));
        verifyNoInteractions(brandMapper);
    }

    @Test
    @DisplayName("Should not update child brand when parent external id is missing")
    void testResolveParentSkipsMissingParent() {
        NhanhBrandDTO childDto = new NhanhBrandDTO(200L, 100L, "C", "Child", 1, 1L);
        Brand child = Brand.builder().id(42L).externalId(200L).name("Child").build();
        when(brandRepo.findByExternalId(200L)).thenReturn(java.util.Optional.of(child));
        when(brandRepo.findByExternalId(100L)).thenReturn(java.util.Optional.empty());

        assertFalse(brandSyncService.resolveParent(childDto));
        verify(brandRepo, never()).save(any(Brand.class));
    }
}
