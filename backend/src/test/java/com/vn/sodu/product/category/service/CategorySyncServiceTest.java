package com.vn.sodu.product.category.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.category.dto.NhanhCategoryDTO;
import com.vn.sodu.product.category.mapper.CategoryMapper;
import com.vn.sodu.product.dto.NhanhResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategorySyncServiceTest {

    @Mock
    private CategoryRepo categoryRepo;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private NhanhService nhanhService;

    @Spy
    @InjectMocks
    private CategorySyncService categorySyncService;

    @Test
    @DisplayName("Should sync parent before child")
    void testSyncCategoriesParentBeforeChild() {
        NhanhCategoryDTO parent = new NhanhCategoryDTO(1L, null, "P", "Parent", 1, null, null, 1);
        NhanhCategoryDTO child = new NhanhCategoryDTO(2L, 1L, "C", "Child", 2, null, null, 1);

        doReturn(List.of(child, parent)).when(categorySyncService).fetchAllCategories();
        doReturn(true).when(categorySyncService).syncOne(any(NhanhCategoryDTO.class));

        categorySyncService.syncCategories();

        var inOrder = inOrder(categorySyncService);
        inOrder.verify(categorySyncService).syncOne(parent);
        inOrder.verify(categorySyncService).syncOne(child);
    }

    @Test
    @DisplayName("Should not loop forever on category cycle")
    void testSyncCategoriesCycleCountsAsFailure() {
        NhanhCategoryDTO a = new NhanhCategoryDTO(1L, 2L, "A", "A", 1, null, null, 1);
        NhanhCategoryDTO b = new NhanhCategoryDTO(2L, 1L, "B", "B", 1, null, null, 1);

        doReturn(List.of(a, b)).when(categorySyncService).fetchAllCategories();

        categorySyncService.syncCategories();

        verify(categorySyncService, never()).syncOne(any());
    }

    @Test
    @DisplayName("Should skip sync when mapper returns null")
    void testSyncOneSkipsWhenMapperNull() {
        NhanhCategoryDTO dto = new NhanhCategoryDTO(1L, null, "C", "Category", 1, null, null, 1);
        when(categoryMapper.toEntity(dto)).thenReturn(null);

        boolean synced = categorySyncService.syncOne(dto);

        assertFalse(synced);
        verify(categoryMapper).toEntity(dto);
        verifyNoInteractions(categoryRepo);
    }

    @Test
    @DisplayName("Should save mapped category")
    void testSyncOneSavesCategory() {
        NhanhCategoryDTO dto = new NhanhCategoryDTO(1L, null, "C", "Category", 1, null, null, 1);
        Category category = Category.builder().id(1L).name("Category").build();
        when(categoryMapper.toEntity(dto)).thenReturn(category);

        boolean synced = categorySyncService.syncOne(dto);

        assertTrue(synced);
        verify(categoryRepo).save(category);
    }

    @Test
    @DisplayName("Should normalize root category parent id from zero to null")
    void testSyncOneNormalizesZeroParentId() {
        NhanhCategoryDTO dto = new NhanhCategoryDTO(1L, 0L, "C", "Category", 1, null, null, 1);
        Category category = Category.builder().id(1L).name("Category").parentId(0L).build();
        when(categoryMapper.toEntity(dto)).thenReturn(category);

        boolean synced = categorySyncService.syncOne(dto);

        assertTrue(synced);
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepo).save(categoryCaptor.capture());
        assertNull(categoryCaptor.getValue().getParentId());
        assertNull(categoryCaptor.getValue().getParent());
    }

    @Test
    @DisplayName("Should preserve positive parent id when saving child category")
    void testSyncOnePreservesPositiveParentId() {
        NhanhCategoryDTO dto = new NhanhCategoryDTO(2L, 1L, "C", "Child", 1, null, null, 1);
        Category category = Category.builder().id(2L).name("Child").build();
        when(categoryMapper.toEntity(dto)).thenReturn(category);

        boolean synced = categorySyncService.syncOne(dto);

        assertTrue(synced);
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepo).save(categoryCaptor.capture());
        assertEquals(1L, categoryCaptor.getValue().getParentId());
        assertNull(categoryCaptor.getValue().getParent());
    }
}
