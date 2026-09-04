package com.vn.sodu.product.brand.service;

import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.audit.AuditService;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.BrandListItemDTO;
import com.vn.sodu.product.brand.dto.BrandRequest;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBrandServiceTest {

    private BrandRepo brandRepo;
    private BrandMapper brandMapper;
    private ProductRepo productRepo;
    private AuditService auditService;
    private AdminBrandService adminBrandService;

    @BeforeEach
    void setUp() {
        brandRepo = mock(BrandRepo.class);
        brandMapper = mock(BrandMapper.class);
        productRepo = mock(ProductRepo.class);
        auditService = mock(AuditService.class);

        adminBrandService = new AdminBrandService(brandRepo, brandMapper, productRepo, auditService);
    }

    @Test
    @DisplayName("Should create brand with IDENTITY id")
    void createBrandGeneratesIdentityId() {
        BrandRequest request = BrandRequest.builder()
                .code("NEW-BRAND")
                .name("New Brand")
                .build();

        Brand savedBrand = Brand.builder().id(1L).code("NEW-BRAND").name("New Brand").build();
        when(brandRepo.save(any(Brand.class))).thenReturn(savedBrand);

        BrandListItemDTO result = adminBrandService.createBrand(request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("BRAND"), eq("1"), any(), any(), eq("Brand created"));
    }

    @Test
    @DisplayName("Should validate required fields on create")
    void createBrandValidatesRequiredFields() {
        BrandRequest request = BrandRequest.builder().build(); // No code, no name

        assertThatThrownBy(() -> adminBrandService.createBrand(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Brand code is required");
    }

    @Test
    @DisplayName("Should reject delete when brand has children")
    void deleteBrandRejectsWhenHasChildren() {
        when(brandRepo.findById(1L)).thenReturn(Optional.of(Brand.builder().id(1L).build()));
        when(brandRepo.existsByParentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminBrandService.deleteBrand(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("child brands");
    }

    @Test
    @DisplayName("Should reject delete when products reference brand")
    void deleteBrandRejectsWhenProductsReference() {
        when(brandRepo.findById(1L)).thenReturn(Optional.of(Brand.builder().id(1L).build()));
        when(brandRepo.existsByParentId(1L)).thenReturn(false);
        when(productRepo.existsByBrandId(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminBrandService.deleteBrand(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("referenced by products");
    }

    @Test
    @DisplayName("Should delete brand when no children and no products")
    void deleteBrandSucceedsWhenNoReferences() {
        Brand brand = Brand.builder().id(1L).code("TEST").name("Test").build();
        when(brandRepo.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepo.existsByParentId(1L)).thenReturn(false);
        when(productRepo.existsByBrandId(1L)).thenReturn(false);

        adminBrandService.deleteBrand(1L);

        verify(brandRepo).delete(brand);
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("BRAND"), eq("1"), any(), eq(null), eq("Brand deleted"));
    }

    @Test
    @DisplayName("Should set brand status")
    void setBrandStatusUpdatesAndAudits() {
        Brand brand = Brand.builder().id(1L).status(1).build();
        when(brandRepo.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepo.save(brand)).thenReturn(brand);

        BrandListItemDTO result = adminBrandService.setBrandStatus(1L, 0);

        assertThat(result.getStatus()).isEqualTo(0);
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("BRAND"), eq("1"), any(), any(), eq("Brand status changed to 0"));
    }

    @Test
    @DisplayName("Should get all brands")
    void getAllBrandsReturnsList() {
        List<Brand> brands = List.of(Brand.builder().id(1L).name("Test").code("TEST").build());
        when(brandRepo.findAll()).thenReturn(brands);

        List<BrandListItemDTO> result = adminBrandService.getAllBrands();

        assertThat(result).hasSize(1);
    }
}