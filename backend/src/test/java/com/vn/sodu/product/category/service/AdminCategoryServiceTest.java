package com.vn.sodu.product.category.service;

import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.audit.AuditService;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.category.dto.CategoryDTO;
import com.vn.sodu.product.category.dto.CategoryListItemDTO;
import com.vn.sodu.product.category.dto.CategoryRequest;
import com.vn.sodu.product.category.mapper.CategoryMapper;
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

class AdminCategoryServiceTest {

    private CategoryRepo categoryRepo;
    private CategoryMapper categoryMapper;
    private ProductRepo productRepo;
    private AuditService auditService;
    private AdminCategoryService adminCategoryService;

    @BeforeEach
    void setUp() {
        categoryRepo = mock(CategoryRepo.class);
        categoryMapper = mock(CategoryMapper.class);
        productRepo = mock(ProductRepo.class);
        auditService = mock(AuditService.class);

        adminCategoryService = new AdminCategoryService(categoryRepo, categoryMapper, productRepo, auditService);
    }

    @Test
    @DisplayName("Should create category with IDENTITY id")
    void createCategoryGeneratesIdentityId() {
        CategoryRequest request = CategoryRequest.builder()
                .code("NEW-CAT")
                .name("New Category")
                .build();

        Category savedCategory = Category.builder().id(1L).code("NEW-CAT").name("New Category").build();
        when(categoryRepo.save(any(Category.class))).thenReturn(savedCategory);
        when(categoryMapper.toDTO(savedCategory)).thenReturn(CategoryDTO.builder().id(1L).code("NEW-CAT").name("New Category").build());

        CategoryDTO result = adminCategoryService.createCategory(request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("CATEGORY"), eq("1"), any(), any(), eq("Category created"));
    }

    @Test
    @DisplayName("Should validate required fields on create")
    void createCategoryValidatesRequiredFields() {
        CategoryRequest request = CategoryRequest.builder().build(); // No code, no name

        assertThatThrownBy(() -> adminCategoryService.createCategory(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Category code is required");
    }

    @Test
    @DisplayName("Should reject delete when category has children")
    void deleteCategoryRejectsWhenHasChildren() {
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).build()));
        when(categoryRepo.existsByParentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminCategoryService.deleteCategory(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("child categories");
    }

    @Test
    @DisplayName("Should reject delete when products reference category")
    void deleteCategoryRejectsWhenProductsReference() {
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).build()));
        when(categoryRepo.existsByParentId(1L)).thenReturn(false);
        when(productRepo.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminCategoryService.deleteCategory(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("referenced by products");
    }

    @Test
    @DisplayName("Should delete category when no children and no products")
    void deleteCategorySucceedsWhenNoReferences() {
        Category category = Category.builder().id(1L).code("TEST").name("Test").build();
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepo.existsByParentId(1L)).thenReturn(false);
        when(productRepo.existsByCategoryId(1L)).thenReturn(false);

        adminCategoryService.deleteCategory(1L);

        verify(categoryRepo).delete(category);
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("CATEGORY"), eq("1"), any(), eq(null), eq("Category deleted"));
    }

    @Test
    @DisplayName("Should set category status")
    void setCategoryStatusUpdatesAndAudits() {
        Category category = Category.builder().id(1L).status(1).build();
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepo.save(category)).thenReturn(category);
        when(categoryMapper.toDTO(category)).thenReturn(CategoryDTO.builder().id(1L).status(0).build());

        CategoryDTO result = adminCategoryService.setCategoryStatus(1L, 0);

        assertThat(result.getStatus()).isEqualTo(0);
        verify(auditService).record(eq(AuditAction.CATALOG_MUTATION), eq("CATEGORY"), eq("1"), any(), any(), eq("Category status changed to 0"));
    }

    @Test
    @DisplayName("Should get all categories")
    void getAllCategoriesReturnsList() {
        List<Category> categories = List.of(Category.builder().id(1L).name("Test").build());
        when(categoryRepo.findAll()).thenReturn(categories);
        when(categoryMapper.toListDTO(any())).thenReturn(CategoryListItemDTO.builder().id(1L).name("Test").build());

        List<CategoryListItemDTO> result = adminCategoryService.getAllCategories();

        assertThat(result).hasSize(1);
    }
}