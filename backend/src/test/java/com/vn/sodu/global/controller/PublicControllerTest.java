package com.vn.sodu.global.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.product.brand.dto.BrandListItemDTO;
import com.vn.sodu.product.brand.service.BrandService;
import com.vn.sodu.product.category.service.CategoryService;
import com.vn.sodu.product.dto.ProductFilterRequest;
import com.vn.sodu.product.dto.ProductListItemDTO;
import com.vn.sodu.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Public Controller Tests")
class PublicControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private BrandService brandService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private PublicController publicController;

    @Test
    void getAllProductsUsesQParamAsSearchFallback() {
        ProductListItemDTO dto = ProductListItemDTO.builder().id(1L).name("Product").build();
        Page<ProductListItemDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(productService.getPublicProducts(any(ProductFilterRequest.class))).thenReturn(page);

        ResponseEntity<PageResponse<ProductListItemDTO>> response =
                publicController.getAllProducts("ao", new ProductFilterRequest());

        ArgumentCaptor<ProductFilterRequest> requestCaptor = ArgumentCaptor.forClass(ProductFilterRequest.class);
        verify(productService).getPublicProducts(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getSearch()).isEqualTo("ao");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void postSearchProductsDelegatesToService() {
        ProductFilterRequest request = new ProductFilterRequest();
        request.setSearch("serum");
        ProductListItemDTO dto = ProductListItemDTO.builder().id(1L).name("Serum").build();
        Page<ProductListItemDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(productService.getPublicProducts(request)).thenReturn(page);

        ResponseEntity<PageResponse<ProductListItemDTO>> response = publicController.searchProducts(request);

        verify(productService).getPublicProducts(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).extracting(ProductListItemDTO::getName).containsExactly("Serum");
    }

    @Test
    void productFilterRequestSupportsQAliasInJsonBody() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        ProductFilterRequest request = objectMapper.readValue("""
                {
                  "q": "mat na",
                  "sortBy": "price",
                  "sortDirection": "ASC"
                }
                """, ProductFilterRequest.class);

        assertThat(request.getSearch()).isEqualTo("mat na");
        assertThat(request.getSortBy()).isEqualTo("price");
        assertThat(request.getSortDirection()).isEqualTo("ASC");
    }

    @Test
    void getAllBrandsReturnsBrandList() {
        BrandListItemDTO dto = BrandListItemDTO.builder()
                .id(10L)
                .code("sobu")
                .name("SoBu")
                .status(1)
                .build();
        when(brandService.getAll()).thenReturn(List.of(dto));

        ResponseEntity<List<BrandListItemDTO>> response = publicController.getAllBrands();

        verify(brandService).getAll();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).extracting(BrandListItemDTO::getName).containsExactly("SoBu");
    }
}
