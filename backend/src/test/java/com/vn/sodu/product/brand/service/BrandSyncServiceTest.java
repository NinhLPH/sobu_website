package com.vn.sodu.product.brand.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.nhanh.service.NhanhClient;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

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
    @DisplayName("Should sync child before parent without FK failure then resolve parent")
    void testSyncBrandsResolvesParentAfterBaseBrands() {
        NhanhBrandDTO childDto = new NhanhBrandDTO(2L, 1L, "C", "Child", 1, 1L);
        NhanhBrandDTO parentDto = new NhanhBrandDTO(1L, null, "P", "Parent", 1, 1L);
        Brand childBase = Brand.builder().id(2L).parentId(1L).name("Child").build();
        Brand parentBase = Brand.builder().id(1L).name("Parent").build();
        Brand childWithParent = Brand.builder().id(2L).parentId(1L).name("Child").build();

        BrandSyncService spyService = spy(brandSyncService);
        doReturn(Arrays.asList(childDto, parentDto)).when(spyService).fetchAllBrands();
        when(brandMapper.toEntity(childDto)).thenReturn(childBase, childWithParent);
        when(brandMapper.toEntity(parentDto)).thenReturn(parentBase);

        spyService.syncBrands();

        InOrder inOrder = inOrder(brandRepo);
        inOrder.verify(brandRepo).save(argThat(brand ->
                brand.getId().equals(2L) && brand.getParentId() == null));
        inOrder.verify(brandRepo).save(argThat(brand -> brand.getId().equals(1L)));
        inOrder.verify(brandRepo).save(argThat(brand ->
                brand.getId().equals(2L) && brand.getParentId().equals(1L)));
    }
}
