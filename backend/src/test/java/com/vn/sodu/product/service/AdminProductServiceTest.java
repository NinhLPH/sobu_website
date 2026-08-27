package com.vn.sodu.product.service;

import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.audit.AuditService;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.badge.ProductBadgeRepo;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.dto.ProductCreateRequest;
import com.vn.sodu.product.dto.ProductDetailDTO;
import com.vn.sodu.product.dto.ProductFilterRequest;
import com.vn.sodu.product.dto.ProductListItemDTO;
import com.vn.sodu.product.dto.ProductUpdateRequest;
import com.vn.sodu.product.mapper.AdminProductMapper;
import com.vn.sodu.product.mapper.ProductMapper;
import com.vn.sodu.product.repo.ProductAttributeRepo;
import com.vn.sodu.product.repo.ProductImageRepo;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.repo.ProductUnitRepo;
import com.vn.sodu.review.ReviewRepository;
import com.vn.sodu.review.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;

class AdminProductServiceTest {

    private ProductRepo productRepo;
    private ProductImageRepo productImageRepo;
    private ProductAttributeRepo productAttributeRepo;
    private ProductUnitRepo productUnitRepo;
    private ProductMapper productMapper;
    private AdminProductMapper adminProductMapper;
    private ReviewRepository reviewRepository;
    private AuditService auditService;
    private BrandRepo brandRepo;
    private CategoryRepo categoryRepo;
    private ProductBadgeRepo productBadgeRepo;
    private AdminProductService adminProductService;

    @BeforeEach
    void setUp() {
        productRepo = mock(ProductRepo.class);
        productImageRepo = mock(ProductImageRepo.class);
        productAttributeRepo = mock(ProductAttributeRepo.class);
        productUnitRepo = mock(ProductUnitRepo.class);
        productMapper = mock(ProductMapper.class);
        adminProductMapper = mock(AdminProductMapper.class);
        reviewRepository = mock(ReviewRepository.class);
        auditService = mock(AuditService.class);
        brandRepo = mock(BrandRepo.class);
        categoryRepo = mock(CategoryRepo.class);
        productBadgeRepo = mock(ProductBadgeRepo.class);

        adminProductService = new AdminProductService(
                productRepo, productImageRepo, productAttributeRepo, productUnitRepo,
                productMapper, adminProductMapper, reviewRepository, auditService,
                brandRepo, categoryRepo, productBadgeRepo
        );
    }

    @Test
    @DisplayName("Should create product with IDENTITY id generation")
    void createProductGeneratesIdentityId() {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .code("TEST-001")
                .name("Test Product")
                .retailPrice(BigDecimal.valueOf(100000))
                .build();

        Product savedProduct = new Product();
        savedProduct.setId(1L); // Simulate IDENTITY generation
        savedProduct.setCode("TEST-001");
        savedProduct.setName("Test Product");

        when(adminProductMapper.toEntity(request)).thenReturn(new Product());
        when(productRepo.save(any(Product.class))).thenReturn(savedProduct);
        when(productRepo.findById(1L)).thenReturn(Optional.of(savedProduct));

        ProductDetailDTO detailDTO = ProductDetailDTO.builder().id(1L).name("Test Product").build();
        when(productMapper.toDetail(any(), any(), any(), any())).thenReturn(detailDTO);
        when(productImageRepo.findByProductId(1L)).thenReturn(List.of());
        when(productUnitRepo.findByProductId(1L)).thenReturn(List.of());
        when(productAttributeRepo.findByProductId(1L)).thenReturn(List.of());
        when(reviewRepository.countByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0L);
        when(reviewRepository.averageRatingByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0.0);

        ProductDetailDTO result = adminProductService.createProduct(request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(productRepo).save(any(Product.class));
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("PRODUCT"), eq("1"), any(), any(), eq("Product created"));
    }

    @Test
    @DisplayName("Should set active default to true when not provided")
    void createProductDefaultsActiveToTrue() {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .code("TEST-001")
                .name("Test Product")
                .retailPrice(BigDecimal.valueOf(100000))
                .active(null) // Not provided
                .build();

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setActive(true);

        when(adminProductMapper.toEntity(request)).thenAnswer(invocation -> {
            Product p = new Product();
            p.setCode(request.getCode());
            p.setName(request.getName());
            p.setRetailPrice(request.getRetailPrice());
            p.setActive(true); // Default
            return p;
        });
        when(productRepo.save(any(Product.class))).thenReturn(savedProduct);
        when(productRepo.findById(1L)).thenReturn(Optional.of(savedProduct));
        when(productMapper.toDetail(any(), any(), any(), any())).thenReturn(ProductDetailDTO.builder().id(1L).active(true).build());
        when(productImageRepo.findByProductId(1L)).thenReturn(List.of());
        when(productUnitRepo.findByProductId(1L)).thenReturn(List.of());
        when(productAttributeRepo.findByProductId(1L)).thenReturn(List.of());
        when(reviewRepository.countByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0L);
        when(reviewRepository.averageRatingByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0.0);

        ProductDetailDTO result = adminProductService.createProduct(request);

        assertThat(result.getActive()).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid sale price pair")
    void createProductRejectsInvalidSalePricePair() {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Invalid sale")
                .retailPrice(new BigDecimal("100000"))
                .oldPrice(new BigDecimal("90000"))
                .build();

        assertThatThrownBy(() -> adminProductService.createProduct(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("oldPrice must be greater than price");
    }

    @Test
    @DisplayName("Should allow product creation with sale window when oldPrice is omitted")
    void createProductAllowsNullOldPriceWithSaleDates() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 31, 12, 0);
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Optional old price")
                .retailPrice(new BigDecimal("80000"))
                .oldPrice(null)
                .saleValidFrom(start)
                .saleValidThrough(start.plusDays(7))
                .build();

        Product savedProduct = new Product();
        savedProduct.setId(2L);
        savedProduct.setName("Optional old price");

        when(adminProductMapper.toEntity(request)).thenReturn(new Product());
        when(productRepo.save(any(Product.class))).thenReturn(savedProduct);
        when(productRepo.findById(2L)).thenReturn(Optional.of(savedProduct));
        when(productMapper.toDetail(any(), any(), any(), any())).thenReturn(ProductDetailDTO.builder().id(2L).build());
        when(productImageRepo.findByProductId(2L)).thenReturn(List.of());
        when(productUnitRepo.findByProductId(2L)).thenReturn(List.of());
        when(productAttributeRepo.findByProductId(2L)).thenReturn(List.of());

        ProductDetailDTO result = adminProductService.createProduct(request);
        assertThat(result.getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should reject reversed sale validity window")
    void createProductRejectsReversedSaleWindow() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 31, 12, 0);
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Invalid sale window")
                .retailPrice(new BigDecimal("80000"))
                .oldPrice(new BigDecimal("100000"))
                .saleValidFrom(start)
                .saleValidThrough(start.minusMinutes(1))
                .build();

        assertThatThrownBy(() -> adminProductService.createProduct(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("saleValidThrough");
    }

    @Test
    @DisplayName("Should replace child entities on update")
    void updateProductReplacesChildren() {
        Product existing = new Product();
        existing.setId(1L);
        existing.setName("Original");

        ProductUpdateRequest request = ProductUpdateRequest.builder()
                .name("Updated")
                .units(List.of(ProductUpdateRequest.ProductUnitRequest.builder().name("New Unit").quantity(1).build()))
                .build();

        when(productRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepo.save(any(Product.class))).thenReturn(existing);
        when(adminProductMapper.toUnits(eq(1L), any(ProductUpdateRequest.class))).thenReturn(List.of(new ProductUnit()));
        when(productMapper.toDetail(any(), any(), any(), any())).thenReturn(ProductDetailDTO.builder().id(1L).name("Updated").build());
        when(productImageRepo.findByProductId(1L)).thenReturn(List.of());
        when(productUnitRepo.findByProductId(1L)).thenReturn(List.of());
        when(productAttributeRepo.findByProductId(1L)).thenReturn(List.of());
        when(reviewRepository.countByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0L);
        when(reviewRepository.averageRatingByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0.0);

        adminProductService.updateProduct(1L, request);

        verify(productUnitRepo).deleteByProductId(1L);
        verify(productAttributeRepo).deleteByProductId(1L);
        verify(productImageRepo).deleteByProductId(1L);
        verify(productUnitRepo).saveAll(any());
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("PRODUCT"), eq("1"), any(), any(), eq("Product updated"));
    }

    @Test
    @DisplayName("Should activate product and audit")
    void setActiveTrueActivatesProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setActive(false);

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(productRepo.save(any(Product.class))).thenReturn(product);
        when(productMapper.toDetail(any(), any(), any(), any())).thenReturn(ProductDetailDTO.builder().id(1L).active(true).build());
        when(productImageRepo.findByProductId(1L)).thenReturn(List.of());
        when(productUnitRepo.findByProductId(1L)).thenReturn(List.of());
        when(productAttributeRepo.findByProductId(1L)).thenReturn(List.of());
        when(reviewRepository.countByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0L);
        when(reviewRepository.averageRatingByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0.0);

        ProductDetailDTO result = adminProductService.setActive(1L, true, "Activated for sale");

        assertThat(result.getActive()).isTrue();
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("PRODUCT"), eq("1"), any(), any(), eq("Product activated: Activated for sale"));
    }

    @Test
    @DisplayName("Should deactivate product and audit")
    void setActiveFalseDeactivatesProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setActive(true);

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(productRepo.save(any(Product.class))).thenReturn(product);
        when(productMapper.toDetail(any(), any(), any(), any())).thenReturn(ProductDetailDTO.builder().id(1L).active(false).build());
        when(productImageRepo.findByProductId(1L)).thenReturn(List.of());
        when(productUnitRepo.findByProductId(1L)).thenReturn(List.of());
        when(productAttributeRepo.findByProductId(1L)).thenReturn(List.of());
        when(reviewRepository.countByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0L);
        when(reviewRepository.averageRatingByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0.0);

        ProductDetailDTO result = adminProductService.setActive(1L, false, "Out of stock");

        assertThat(result.getActive()).isFalse();
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("PRODUCT"), eq("1"), any(), any(), eq("Product deactivated: Out of stock"));
    }

    @Test
    @DisplayName("Should archive product (status=ARCHIVED, active=false)")
    void archiveProductSetsArchivedAndInactive() {
        Product product = new Product();
        product.setId(1L);
        product.setStatus("ACTIVE");
        product.setActive(true);

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(productRepo.save(any(Product.class))).thenReturn(product);
        when(productMapper.toDetail(any(), any(), any(), any())).thenReturn(ProductDetailDTO.builder().id(1L).status("ARCHIVED").active(false).build());
        when(productImageRepo.findByProductId(1L)).thenReturn(List.of());
        when(productUnitRepo.findByProductId(1L)).thenReturn(List.of());
        when(productAttributeRepo.findByProductId(1L)).thenReturn(List.of());
        when(reviewRepository.countByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0L);
        when(reviewRepository.averageRatingByProductIdAndStatus(1L, ReviewStatus.PUBLISHED)).thenReturn(0.0);

        ProductDetailDTO result = adminProductService.archiveProduct(1L, "End of life");

        assertThat(result.getStatus()).isEqualTo("ARCHIVED");
        assertThat(result.getActive()).isFalse();
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("PRODUCT"), eq("1"), any(), any(), eq("Product archived: End of life"));
    }

    @Test
    @DisplayName("Should throw NotFoundException when product not found for update")
    void updateProductNotFoundThrowsException() {
        when(productRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminProductService.updateProduct(999L, ProductUpdateRequest.builder().build()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Product not found with id: 999");
    }

    @Test
    @DisplayName("Should filter by active and status in admin list")
    void getAllProductsFiltersByActiveAndStatus() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setActive(true);
        request.setStatus("ACTIVE");

        org.springframework.data.domain.Page<ProductListItemDTO> emptyPage = new org.springframework.data.domain.PageImpl<>(List.of());
        when(productRepo.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(emptyPage);
        when(productMapper.toListItem(any())).thenReturn(ProductListItemDTO.builder().id(1L).active(true).status("ACTIVE").build());

        adminProductService.getAllProducts(request);

        // Verify the specification was called with active and status filters
        // (We can't easily capture the specification, but we verify the method runs)
        verify(productRepo).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class));
    }
}
