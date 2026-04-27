package com.vn.sodu.product.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.mapper.ProductMapper;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.repo.ProductAttributeRepo;
import com.vn.sodu.product.repo.ProductImageRepo;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.repo.ProductUnitRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductSyncServiceUpsertTest {

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
    private com.vn.sodu.product.service.ProductSyncService productSyncService;

    @Test
    void syncOne_preservesExistingId_whenExternalIdFound() {
        NhanhProductDTO dto = new NhanhProductDTO();
        dto.setId(123L);

        Product existing = new Product();
        existing.setId(999L);
        existing.setExternalId(123L);

        when(productRepo.findByExternalId(123L)).thenReturn(Optional.of(existing));

        Product mapped = new Product();
        mapped.setId(123L);
        when(productMapper.toEntity(dto)).thenReturn(mapped);

        doNothing().when(productUnitRepo).deleteByProductId(anyLong());
        // return a non-empty child list so saveAll is invoked
        when(productMapper.toUnits(anyLong(), any())).thenReturn(java.util.List.of(new com.vn.sodu.product.ProductUnit()));
        when(productMapper.toAttributes(anyLong(), any())).thenReturn(java.util.List.of(new com.vn.sodu.product.ProductAttribute()));
        when(productMapper.toImages(anyLong(), any())).thenReturn(java.util.List.of(new com.vn.sodu.product.ProductImage()));

        productSyncService.syncOne(dto);

        // verify saved product has existing PK (999)
        verify(productRepo).save(argThat(p -> p.getId().equals(999L) && p.getExternalId().equals(123L)));
        verify(productUnitRepo).deleteByProductId(999L);
        verify(productAttributeRepo).deleteByProductId(999L);
        verify(productImageRepo).deleteByProductId(999L);
        verify(productUnitRepo).saveAll(anyList());
    }
}
