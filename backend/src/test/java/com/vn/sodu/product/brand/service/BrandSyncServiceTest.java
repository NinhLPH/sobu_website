package com.vn.sodu.product.brand.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import com.vn.sodu.product.dto.NhanhResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
}
