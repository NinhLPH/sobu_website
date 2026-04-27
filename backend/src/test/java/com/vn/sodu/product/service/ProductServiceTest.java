package com.vn.sodu.product.service;

import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.dto.ProductDetailDTO;
import com.vn.sodu.product.dto.ProductFilterRequest;
import com.vn.sodu.product.dto.ProductListItemDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service Tests")
class ProductServiceTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private ProductImageRepo productImageRepo;

    @Mock
    private ProductAttributeRepo productAttributeRepo;

    @Mock
    private ProductUnitRepo productUnitRepo;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductListItemDTO testListItemDTO;
    private ProductDetailDTO testDetailDTO;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setCode("TEST-001");
        testProduct.setName("Test Product");
        testProduct.setRetailPrice(BigDecimal.valueOf(100));
        testProduct.setStockAvailable(10.0);

        testListItemDTO = new ProductListItemDTO();
        testListItemDTO.setId(1L);
        testListItemDTO.setName("Test Product");

        testDetailDTO = new ProductDetailDTO();
        testDetailDTO.setId(1L);
        testDetailDTO.setName("Test Product");
    }

    // ──── Test getAllProducts() ──────────────────────────────────────────────

    @Test
    @DisplayName("Should return all products")
    void testGetAllProducts() {
        List<Product> products = Arrays.asList(testProduct, createTestProduct(2L));

        when(productRepo.findAll()).thenReturn(products);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO)
                .thenReturn(createTestListItemDTO(2L));

        List<ProductListItemDTO> result = productService.getAllProducts();

        assertEquals(2, result.size());
        verify(productRepo).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no products exist")
    void testGetAllProductsEmpty() {
        when(productRepo.findAll()).thenReturn(new ArrayList<>());

        List<ProductListItemDTO> result = productService.getAllProducts();

        assertEquals(0, result.size());
    }

    // ──── Test getPublicProducts() ───────────────────────────────────────────

    @Test
    @DisplayName("Should get public products with default pagination")
    void testGetPublicProductsWithDefaults() {
        ProductFilterRequest request = new ProductFilterRequest();
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(request);

        assertEquals(1, result.getContent().size());
        verify(productRepo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get public products with null request")
    void testGetPublicProductsWithNullRequest() {
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(null);

        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should filter products by category")
    void testGetPublicProductsFilterByCategory() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setCategoryId(1L);
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(request);

        assertEquals(1, result.getContent().size());
        verify(productRepo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Should filter products by price range")
    void testGetPublicProductsFilterByPrice() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setMinPrice(BigDecimal.valueOf(50));
        request.setMaxPrice(BigDecimal.valueOf(150));
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(request);

        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should filter products by stock availability")
    void testGetPublicProductsFilterByStock() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setInStock(true);
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(request);

        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should filter products by search term")
    void testGetPublicProductsFilterBySearch() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setSearch("Test");
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(request);

        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should apply default page size when page size exceeds maximum")
    void testGetPublicProductsPageSizeLimit() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setPageSize(200);
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(request);

        assertEquals(1, result.getContent().size());
    }

    // ──── Test searchProducts() ──────────────────────────────────────────────

    @Test
    @DisplayName("Should search products by term")
    void testSearchProducts() {
        ProductFilterRequest request = new ProductFilterRequest();
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.searchProducts("Test", request);

        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should search products with null request")
    void testSearchProductsWithNullRequest() {
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.searchProducts("Test", null);

        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should search with empty results")
    void testSearchProductsNoResults() {
        ProductFilterRequest request = new ProductFilterRequest();
        Page<Product> page = new PageImpl<>(new ArrayList<>());

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<ProductListItemDTO> result = productService.searchProducts("NonExistent", request);

        assertEquals(0, result.getContent().size());
    }

    // ──── Test getProductDetailById() ────────────────────────────────────────

    @Test
    @DisplayName("Should get product detail by id")
    void testGetProductDetailById() {
        List<ProductImage> images = Arrays.asList(new ProductImage());
        List<ProductUnit> units = Arrays.asList(new ProductUnit());
        List<ProductAttribute> attributes = Arrays.asList(new ProductAttribute());

        when(productRepo.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productImageRepo.findByProductId(1L)).thenReturn(images);
        when(productUnitRepo.findByProductId(1L)).thenReturn(units);
        when(productAttributeRepo.findByProductId(1L)).thenReturn(attributes);
        when(productMapper.toDetail(
                eq(testProduct),
                eq(units),
                eq(attributes),
                eq(images)
        )).thenReturn(testDetailDTO);

        ProductDetailDTO result = productService.getProductDetailById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(productRepo).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void testGetProductDetailByIdNotFound() {
        when(productRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.getProductDetailById(999L));
    }

    @Test
    @DisplayName("Should get product detail with empty images")
    void testGetProductDetailByIdWithEmptyImages() {
        List<ProductImage> images = new ArrayList<>();
        List<ProductUnit> units = Arrays.asList(new ProductUnit());
        List<ProductAttribute> attributes = Arrays.asList(new ProductAttribute());

        when(productRepo.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productImageRepo.findByProductId(1L)).thenReturn(images);
        when(productUnitRepo.findByProductId(1L)).thenReturn(units);
        when(productAttributeRepo.findByProductId(1L)).thenReturn(attributes);
        when(productMapper.toDetail(
                eq(testProduct),
                eq(units),
                eq(attributes),
                eq(images)
        )).thenReturn(testDetailDTO);

        ProductDetailDTO result = productService.getProductDetailById(1L);

        assertNotNull(result);
        verify(productImageRepo).findByProductId(1L);
    }

    @Test
    @DisplayName("Should get product detail with empty units and attributes")
    void testGetProductDetailByIdWithEmptyUnitsAndAttributes() {
        List<ProductImage> images = Arrays.asList(new ProductImage());
        List<ProductUnit> units = new ArrayList<>();
        List<ProductAttribute> attributes = new ArrayList<>();

        when(productRepo.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productImageRepo.findByProductId(1L)).thenReturn(images);
        when(productUnitRepo.findByProductId(1L)).thenReturn(units);
        when(productAttributeRepo.findByProductId(1L)).thenReturn(attributes);
        when(productMapper.toDetail(
                eq(testProduct),
                eq(units),
                eq(attributes),
                eq(images)
        )).thenReturn(testDetailDTO);

        ProductDetailDTO result = productService.getProductDetailById(1L);

        assertNotNull(result);
        verify(productUnitRepo).findByProductId(1L);
    }

    // ──── Sorting and Pagination Tests ───────────────────────────────────────

    @Test
    @DisplayName("Should apply DESC sort direction by default")
    void testGetPublicProductsDefaultSort() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setSortBy("id");
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(request);

        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should apply default page number 0 when negative")
    void testGetPublicProductsNegativePageNumber() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setPage(-1);
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(request);

        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should handle invalid sort direction")
    void testGetPublicProductsInvalidSortDirection() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setSortDirection("INVALID");
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(request);

        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("Should handle multiple filters combined")
    void testGetPublicProductsMultipleFilters() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setCategoryId(1L);
        request.setBrandId(1L);
        request.setMinPrice(BigDecimal.valueOf(50));
        request.setMaxPrice(BigDecimal.valueOf(150));
        request.setInStock(true);
        request.setSearch("Test");

        List<Product> products = Arrays.asList(testProduct);
        Page<Product> page = new PageImpl<>(products);

        when(productRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toListItem(any(Product.class)))
                .thenReturn(testListItemDTO);

        Page<ProductListItemDTO> result = productService.getPublicProducts(request);

        assertEquals(1, result.getContent().size());
    }

    // ──── Helper Methods ─────────────────────────────────────────────────────

    private Product createTestProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setCode("TEST-" + String.format("%03d", id));
        product.setName("Test Product " + id);
        product.setRetailPrice(BigDecimal.valueOf(100));
        product.setStockAvailable(10.0);
        return product;
    }

    private ProductListItemDTO createTestListItemDTO(Long id) {
        ProductListItemDTO dto = new ProductListItemDTO();
        dto.setId(id);
        dto.setName("Test Product " + id);
        return dto;
    }
}
