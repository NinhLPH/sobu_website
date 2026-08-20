package com.vn.sodu.product.controller;

import com.vn.sodu.product.dto.ProductCreateRequest;
import com.vn.sodu.product.dto.ProductDetailDTO;
import com.vn.sodu.product.dto.ProductFilterRequest;
import com.vn.sodu.product.dto.ProductListItemDTO;
import com.vn.sodu.product.dto.ProductUpdateRequest;
import com.vn.sodu.product.service.AdminProductService;
import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.global.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminProductControllerTest {

    @Test
    @DisplayName("Should allow staff to create product")
    void createProductAllowsStaff() {
        AdminProductService service = mock(AdminProductService.class);
        AdminProductController controller = new AdminProductController(service);

        ProductCreateRequest request = ProductCreateRequest.builder()
                .code("TEST-001")
                .name("Test Product")
                .retailPrice(BigDecimal.valueOf(100000))
                .build();

        ProductDetailDTO dto = ProductDetailDTO.builder()
                .id(1L)
                .code("TEST-001")
                .name("Test Product")
                .build();

        when(service.createProduct(request)).thenReturn(dto);

        var response = controller.createProduct(staffAuth(), request);

        verify(service).createProduct(request);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should reject non-staff for create product")
    void createProductRejectsNonStaff() {
        AdminProductService service = mock(AdminProductService.class);
        AdminProductController controller = new AdminProductController(service);

        ProductCreateRequest request = ProductCreateRequest.builder().code("TEST").name("Test").build();

        assertThatThrownBy(() -> controller.createProduct(userAuth(), request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should allow staff to get all products")
    void getAllProductsAllowsStaff() {
        AdminProductService service = mock(AdminProductService.class);
        AdminProductController controller = new AdminProductController(service);

        ProductFilterRequest request = new ProductFilterRequest();
        PageResponse<ProductListItemDTO> page = PageResponse.from(
                new org.springframework.data.domain.PageImpl<>(List.of(
                        ProductListItemDTO.builder().id(1L).name("Test").build()
                ))
        );

        when(service.getAllProducts(request)).thenReturn(page);

        var response = controller.getAllProducts(staffAuth(), request);

        verify(service).getAllProducts(request);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(page);
    }

    @Test
    @DisplayName("Should reject non-staff for get all products")
    void getAllProductsRejectsNonStaff() {
        AdminProductService service = mock(AdminProductService.class);
        AdminProductController controller = new AdminProductController(service);

        assertThatThrownBy(() -> controller.getAllProducts(userAuth(), new ProductFilterRequest()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should allow staff to get product by id")
    void getProductByIdAllowsStaff() {
        AdminProductService service = mock(AdminProductService.class);
        AdminProductController controller = new AdminProductController(service);

        ProductDetailDTO dto = ProductDetailDTO.builder().id(1L).name("Test").build();
        when(service.getProductDetailById(1L)).thenReturn(dto);

        var response = controller.getProductById(staffAuth(), 1L);

        verify(service).getProductDetailById(1L);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should allow staff to update product")
    void updateProductAllowsStaff() {
        AdminProductService service = mock(AdminProductService.class);
        AdminProductController controller = new AdminProductController(service);

        ProductUpdateRequest request = ProductUpdateRequest.builder().name("Updated").build();
        ProductDetailDTO dto = ProductDetailDTO.builder().id(1L).name("Updated").build();
        when(service.updateProduct(1L, request)).thenReturn(dto);

        var response = controller.updateProduct(staffAuth(), 1L, request);

        verify(service).updateProduct(1L, request);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should allow staff to set product active")
    void setActiveAllowsStaff() {
        AdminProductService service = mock(AdminProductService.class);
        AdminProductController controller = new AdminProductController(service);

        AdminProductController.ActiveRequest request = new AdminProductController.ActiveRequest();
        request.setActive(true);
        request.setReason("Test reason");

        ProductDetailDTO dto = ProductDetailDTO.builder().id(1L).name("Test").active(true).build();
        when(service.setActive(1L, true, "Test reason")).thenReturn(dto);

        var response = controller.setActive(staffAuth(), 1L, request);

        verify(service).setActive(1L, true, "Test reason");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should reject setActive with null active field")
    void setActiveRejectsNullActive() {
        AdminProductService service = mock(AdminProductService.class);
        AdminProductController controller = new AdminProductController(service);

        AdminProductController.ActiveRequest request = new AdminProductController.ActiveRequest();
        request.setReason("Test reason");
        // active is null

        assertThatThrownBy(() -> controller.setActive(staffAuth(), 1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should allow staff to archive product")
    void archiveProductAllowsStaff() {
        AdminProductService service = mock(AdminProductService.class);
        AdminProductController controller = new AdminProductController(service);

        AdminProductController.ArchiveRequest request = new AdminProductController.ArchiveRequest();
        request.setReason("Archived");

        ProductDetailDTO dto = ProductDetailDTO.builder().id(1L).name("Test").status("ARCHIVED").active(false).build();
        when(service.archiveProduct(1L, "Archived")).thenReturn(dto);

        var response = controller.archiveProduct(staffAuth(), 1L, request);

        verify(service).archiveProduct(1L, "Archived");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    private TestingAuthenticationToken staffAuth() {
        return new TestingAuthenticationToken("staff@sobu.vn", null, "ROLE_STAFF");
    }

    private TestingAuthenticationToken userAuth() {
        return new TestingAuthenticationToken("user@sobu.vn", null, "ROLE_USER");
    }
}