package com.vn.sodu.product.service;

import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.mapper.ProductMapper;
import com.vn.sodu.product.repo.ProductAttributeRepo;
import com.vn.sodu.product.repo.ProductImageRepo;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.repo.ProductUnitRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Sync Service Tests")
class ProductSyncServiceTest {

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

    @InjectMocks
    private ProductSyncService productSyncService;

    private NhanhProductDTO testProduct;
    private Product testProductEntity;

    @BeforeEach
    void setUp() {
        testProduct = new NhanhProductDTO();
        testProduct.setId(1L);
        testProduct.setCode("TEST-001");
        testProduct.setName("Test Product");
        testProduct.setStatus("active");
        testProduct.setDescription("Test Description");

        testProductEntity = new Product();
        testProductEntity.setId(1L);
        testProductEntity.setCode("TEST-001");
        testProductEntity.setName("Test Product");
    }

    // ──── Test syncOne() ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should sync single product with units, attributes, and images")
    void testSyncOneWithAllData() {
        List<ProductUnit> units = Arrays.asList(new ProductUnit(), new ProductUnit());
        List<ProductAttribute> attributes = Arrays.asList(new ProductAttribute());
        List<ProductImage> images = Arrays.asList(new ProductImage());

        when(productMapper.toEntity(testProduct)).thenReturn(testProductEntity);
        when(productRepo.save(any(Product.class))).thenReturn(testProductEntity);
        when(productMapper.toUnits(1L, testProduct)).thenReturn(units);
        when(productMapper.toAttributes(1L, testProduct)).thenReturn(attributes);
        when(productMapper.toImages(1L, testProduct)).thenReturn(images);

        productSyncService.syncOne(testProduct);

        verify(productRepo).save(testProductEntity);
        verify(productUnitRepo).deleteByProductId(1L);
        verify(productAttributeRepo).deleteByProductId(1L);
        verify(productImageRepo).deleteByProductId(1L);
        verify(productUnitRepo).saveAll(units);
        verify(productAttributeRepo).saveAll(attributes);
        verify(productImageRepo).saveAll(images);
    }

    @Test
    @DisplayName("Should handle null product DTO in syncOne")
    void testSyncOneWithNullProduct() {
        productSyncService.syncOne(null);

        verify(productRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should handle null product ID in syncOne")
    void testSyncOneWithNullProductId() {
        testProduct.setId(null);

        productSyncService.syncOne(testProduct);

        verify(productRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should delete old children before inserting new ones")
    void testSyncOneDeletesOldData() {
        when(productMapper.toEntity(testProduct)).thenReturn(testProductEntity);
        when(productRepo.save(any(Product.class))).thenReturn(testProductEntity);
        when(productMapper.toUnits(1L, testProduct)).thenReturn(null);
        when(productMapper.toAttributes(1L, testProduct)).thenReturn(null);
        when(productMapper.toImages(1L, testProduct)).thenReturn(null);

        productSyncService.syncOne(testProduct);

        verify(productUnitRepo).deleteByProductId(1L);
        verify(productAttributeRepo).deleteByProductId(1L);
        verify(productImageRepo).deleteByProductId(1L);
    }

    @Test
    @DisplayName("Should not save empty child collections")
    void testSyncOneWithEmptyChildCollections() {
        when(productMapper.toEntity(testProduct)).thenReturn(testProductEntity);
        when(productRepo.save(any(Product.class))).thenReturn(testProductEntity);
        when(productMapper.toUnits(1L, testProduct)).thenReturn(new ArrayList<>());
        when(productMapper.toAttributes(1L, testProduct)).thenReturn(new ArrayList<>());
        when(productMapper.toImages(1L, testProduct)).thenReturn(new ArrayList<>());

        productSyncService.syncOne(testProduct);

        verify(productUnitRepo, never()).saveAll(any());
        verify(productAttributeRepo, never()).saveAll(any());
        verify(productImageRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should handle sync with partial data")
    void testSyncOneWithPartialData() {
        List<ProductUnit> units = Arrays.asList(new ProductUnit());

        when(productMapper.toEntity(testProduct)).thenReturn(testProductEntity);
        when(productRepo.save(any(Product.class))).thenReturn(testProductEntity);
        when(productMapper.toUnits(1L, testProduct)).thenReturn(units);
        when(productMapper.toAttributes(1L, testProduct)).thenReturn(null);
        when(productMapper.toImages(1L, testProduct)).thenReturn(null);

        productSyncService.syncOne(testProduct);

        verify(productUnitRepo).saveAll(units);
        verify(productAttributeRepo, never()).saveAll(any());
        verify(productImageRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should save product with mapper conversion")
    void testSyncOneConvertsAndSaves() {
        Product convertedProduct = new Product();
        convertedProduct.setId(1L);
        convertedProduct.setCode("CONVERTED-001");

        when(productMapper.toEntity(testProduct)).thenReturn(convertedProduct);
        when(productRepo.save(convertedProduct)).thenReturn(convertedProduct);
        when(productMapper.toUnits(1L, testProduct)).thenReturn(null);
        when(productMapper.toAttributes(1L, testProduct)).thenReturn(null);
        when(productMapper.toImages(1L, testProduct)).thenReturn(null);

        productSyncService.syncOne(testProduct);

        verify(productMapper).toEntity(testProduct);
        verify(productRepo).save(convertedProduct);
    }

    @Test
    @DisplayName("Should handle mapper returning null entity")
    void testSyncOneWithMapperReturningNull() {
        when(productMapper.toEntity(testProduct)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> productSyncService.syncOne(testProduct));
    }

    @Test
    @DisplayName("Should process multiple syncOne calls independently")
    void testMultipleSyncOneCalls() {
        NhanhProductDTO product2 = createTestProduct(2L);
        Product entity2 = createTestProductEntity(2L);

        when(productMapper.toEntity(testProduct)).thenReturn(testProductEntity);
        when(productMapper.toEntity(product2)).thenReturn(entity2);
        when(productRepo.save(any(Product.class))).thenReturn(testProductEntity).thenReturn(entity2);
        when(productMapper.toUnits(anyLong(), any())).thenReturn(null);
        when(productMapper.toAttributes(anyLong(), any())).thenReturn(null);
        when(productMapper.toImages(anyLong(), any())).thenReturn(null);

        productSyncService.syncOne(testProduct);
        productSyncService.syncOne(product2);

        verify(productRepo, times(2)).save(any());
        verify(productUnitRepo, times(2)).deleteByProductId(anyLong());
    }

    // ──── Helper Methods ────────────────────────────────────────────────────

    private NhanhProductDTO createTestProduct(Long id) {
        NhanhProductDTO product = new NhanhProductDTO();
        product.setId(id);
        product.setCode("TEST-" + String.format("%03d", id));
        product.setName("Test Product " + id);
        product.setStatus("active");
        return product;
    }

    private Product createTestProductEntity(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setCode("TEST-" + String.format("%03d", id));
        product.setName("Test Product " + id);
        return product;
    }
}
