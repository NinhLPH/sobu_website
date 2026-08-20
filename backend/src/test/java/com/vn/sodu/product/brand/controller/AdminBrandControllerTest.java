package com.vn.sodu.product.brand.controller;

import com.vn.sodu.product.brand.dto.BrandListItemDTO;
import com.vn.sodu.product.brand.dto.BrandRequest;
import com.vn.sodu.product.brand.service.AdminBrandService;
import com.vn.sodu.global.dto.ApiResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBrandControllerTest {

    @Test
    @DisplayName("Should allow staff to get all brands")
    void getAllBrandsAllowsStaff() {
        AdminBrandService service = mock(AdminBrandService.class);
        AdminBrandController controller = new AdminBrandController(service);

        List<BrandListItemDTO> brands = List.of(
                BrandListItemDTO.builder().id(1L).name("Test").code("TEST").build()
        );
        when(service.getAllBrands()).thenReturn(brands);

        var response = controller.getAllBrands(staffAuth());

        verify(service).getAllBrands();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(brands);
    }

    @Test
    @DisplayName("Should reject non-staff for get all brands")
    void getAllBrandsRejectsNonStaff() {
        AdminBrandService service = mock(AdminBrandService.class);
        AdminBrandController controller = new AdminBrandController(service);

        assertThatThrownBy(() -> controller.getAllBrands(userAuth()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should allow staff to get brand by id")
    void getBrandByIdAllowsStaff() {
        AdminBrandService service = mock(AdminBrandService.class);
        AdminBrandController controller = new AdminBrandController(service);

        BrandListItemDTO dto = BrandListItemDTO.builder().id(1L).name("Test").code("TEST").build();
        when(service.getBrandById(1L)).thenReturn(dto);

        var response = controller.getBrandById(staffAuth(), 1L);

        verify(service).getBrandById(1L);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should allow staff to create brand")
    void createBrandAllowsStaff() {
        AdminBrandService service = mock(AdminBrandService.class);
        AdminBrandController controller = new AdminBrandController(service);

        BrandRequest request = BrandRequest.builder().code("NEW").name("New Brand").build();
        BrandListItemDTO dto = BrandListItemDTO.builder().id(1L).code("NEW").name("New Brand").build();
        when(service.createBrand(request)).thenReturn(dto);

        var response = controller.createBrand(staffAuth(), request);

        verify(service).createBrand(request);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should reject non-staff for create brand")
    void createBrandRejectsNonStaff() {
        AdminBrandService service = mock(AdminBrandService.class);
        AdminBrandController controller = new AdminBrandController(service);

        BrandRequest request = BrandRequest.builder().code("NEW").name("New").build();

        assertThatThrownBy(() -> controller.createBrand(userAuth(), request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should allow staff to update brand")
    void updateBrandAllowsStaff() {
        AdminBrandService service = mock(AdminBrandService.class);
        AdminBrandController controller = new AdminBrandController(service);

        BrandRequest request = BrandRequest.builder().code("UPD").name("Updated").build();
        BrandListItemDTO dto = BrandListItemDTO.builder().id(1L).code("UPD").name("Updated").build();
        when(service.updateBrand(1L, request)).thenReturn(dto);

        var response = controller.updateBrand(staffAuth(), 1L, request);

        verify(service).updateBrand(1L, request);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should allow staff to delete brand")
    void deleteBrandAllowsStaff() {
        AdminBrandService service = mock(AdminBrandService.class);
        AdminBrandController controller = new AdminBrandController(service);

        var response = controller.deleteBrand(staffAuth(), 1L);

        verify(service).deleteBrand(1L);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Brand deleted successfully");
    }

    @Test
    @DisplayName("Should allow staff to set brand status")
    void setBrandStatusAllowsStaff() {
        AdminBrandService service = mock(AdminBrandService.class);
        AdminBrandController controller = new AdminBrandController(service);

        AdminBrandController.StatusRequest request = new AdminBrandController.StatusRequest();
        request.setStatus(0);

        BrandListItemDTO dto = BrandListItemDTO.builder().id(1L).name("Test").status(0).build();
        when(service.setBrandStatus(1L, 0)).thenReturn(dto);

        var response = controller.setBrandStatus(staffAuth(), 1L, request);

        verify(service).setBrandStatus(1L, 0);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should reject setBrandStatus with null status")
    void setBrandStatusRejectsNullStatus() {
        AdminBrandService service = mock(AdminBrandService.class);
        AdminBrandController controller = new AdminBrandController(service);

        AdminBrandController.StatusRequest request = new AdminBrandController.StatusRequest();
        // status is null

        assertThatThrownBy(() -> controller.setBrandStatus(staffAuth(), 1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TestingAuthenticationToken staffAuth() {
        return new TestingAuthenticationToken("staff@sobu.vn", null, "ROLE_STAFF");
    }

    private TestingAuthenticationToken userAuth() {
        return new TestingAuthenticationToken("user@sobu.vn", null, "ROLE_USER");
    }
}