package com.vn.sodu.product.category.controller;

import com.vn.sodu.product.category.dto.CategoryDTO;
import com.vn.sodu.product.category.dto.CategoryListItemDTO;
import com.vn.sodu.product.category.dto.CategoryRequest;
import com.vn.sodu.product.category.service.AdminCategoryService;
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

class AdminCategoryControllerTest {

    @Test
    @DisplayName("Should allow staff to get all categories")
    void getAllCategoriesAllowsStaff() {
        AdminCategoryService service = mock(AdminCategoryService.class);
        AdminCategoryController controller = new AdminCategoryController(service);

        List<CategoryListItemDTO> categories = List.of(
                CategoryListItemDTO.builder().id(1L).name("Test").code("TEST").build()
        );
        when(service.getAllCategories()).thenReturn(categories);

        var response = controller.getAllCategories(staffAuth());

        verify(service).getAllCategories();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(categories);
    }

    @Test
    @DisplayName("Should reject non-staff for get all categories")
    void getAllCategoriesRejectsNonStaff() {
        AdminCategoryService service = mock(AdminCategoryService.class);
        AdminCategoryController controller = new AdminCategoryController(service);

        assertThatThrownBy(() -> controller.getAllCategories(userAuth()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should allow staff to get category by id")
    void getCategoryByIdAllowsStaff() {
        AdminCategoryService service = mock(AdminCategoryService.class);
        AdminCategoryController controller = new AdminCategoryController(service);

        CategoryDTO dto = CategoryDTO.builder().id(1L).name("Test").code("TEST").build();
        when(service.getCategoryById(1L)).thenReturn(dto);

        var response = controller.getCategoryById(staffAuth(), 1L);

        verify(service).getCategoryById(1L);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should allow staff to create category")
    void createCategoryAllowsStaff() {
        AdminCategoryService service = mock(AdminCategoryService.class);
        AdminCategoryController controller = new AdminCategoryController(service);

        CategoryRequest request = CategoryRequest.builder().code("NEW").name("New Category").build();
        CategoryDTO dto = CategoryDTO.builder().id(1L).code("NEW").name("New Category").build();
        when(service.createCategory(request)).thenReturn(dto);

        var response = controller.createCategory(staffAuth(), request);

        verify(service).createCategory(request);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should reject non-staff for create category")
    void createCategoryRejectsNonStaff() {
        AdminCategoryService service = mock(AdminCategoryService.class);
        AdminCategoryController controller = new AdminCategoryController(service);

        CategoryRequest request = CategoryRequest.builder().code("NEW").name("New").build();

        assertThatThrownBy(() -> controller.createCategory(userAuth(), request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should allow staff to update category")
    void updateCategoryAllowsStaff() {
        AdminCategoryService service = mock(AdminCategoryService.class);
        AdminCategoryController controller = new AdminCategoryController(service);

        CategoryRequest request = CategoryRequest.builder().code("UPD").name("Updated").build();
        CategoryDTO dto = CategoryDTO.builder().id(1L).code("UPD").name("Updated").build();
        when(service.updateCategory(1L, request)).thenReturn(dto);

        var response = controller.updateCategory(staffAuth(), 1L, request);

        verify(service).updateCategory(1L, request);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should allow staff to delete category")
    void deleteCategoryAllowsStaff() {
        AdminCategoryService service = mock(AdminCategoryService.class);
        AdminCategoryController controller = new AdminCategoryController(service);

        var response = controller.deleteCategory(staffAuth(), 1L);

        verify(service).deleteCategory(1L);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Category deleted successfully");
    }

    @Test
    @DisplayName("Should allow staff to set category status")
    void setCategoryStatusAllowsStaff() {
        AdminCategoryService service = mock(AdminCategoryService.class);
        AdminCategoryController controller = new AdminCategoryController(service);

        AdminCategoryController.StatusRequest request = new AdminCategoryController.StatusRequest();
        request.setStatus(0);

        CategoryDTO dto = CategoryDTO.builder().id(1L).name("Test").status(0).build();
        when(service.setCategoryStatus(1L, 0)).thenReturn(dto);

        var response = controller.setCategoryStatus(staffAuth(), 1L, request);

        verify(service).setCategoryStatus(1L, 0);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    @DisplayName("Should reject setCategoryStatus with null status")
    void setCategoryStatusRejectsNullStatus() {
        AdminCategoryService service = mock(AdminCategoryService.class);
        AdminCategoryController controller = new AdminCategoryController(service);

        AdminCategoryController.StatusRequest request = new AdminCategoryController.StatusRequest();
        // status is null

        assertThatThrownBy(() -> controller.setCategoryStatus(staffAuth(), 1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TestingAuthenticationToken staffAuth() {
        return new TestingAuthenticationToken("staff@sobu.vn", null, "ROLE_STAFF");
    }

    private TestingAuthenticationToken userAuth() {
        return new TestingAuthenticationToken("user@sobu.vn", null, "ROLE_USER");
    }
}