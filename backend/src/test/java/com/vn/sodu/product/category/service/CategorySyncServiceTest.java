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
    @DisplayName("Should build category URL from base URL, client id, and business id")
    void testBuildCategoryListUrl() {
        ReflectionTestUtils.setField(categorySyncService, "nhanhBaseUrl", "https://pos.open.nhanh.vn/api");
        ReflectionTestUtils.setField(categorySyncService, "clientId", "77323");
        ReflectionTestUtils.setField(categorySyncService, "businessId", "224003");

        String url = ReflectionTestUtils.invokeMethod(categorySyncService, "buildCategoryListUrl");

        assertEquals("https://pos.open.nhanh.vn/v3.0/product/category?appId=77323&businessId=224003", url);
    }

    @Test
    @DisplayName("Should build category request body with paginator")
    void testBuildCategoryListRequestBody() {
        Map<String, Object> body = ReflectionTestUtils.invokeMethod(categorySyncService, "buildCategoryListRequestBody", (Object) null);

        assertTrue(body.containsKey("paginator"));
        Map<String, Object> paginator = (Map<String, Object>) body.get("paginator");
        assertEquals(50, paginator.get("size"));
        assertFalse(paginator.containsKey("next"));
    }

    @Test
    @DisplayName("Should collect categories across next cursors")
    void testFetchAllCategoriesMultiPage() {
        NhanhCategoryDTO parent = new NhanhCategoryDTO(10L, null, "C1", "Parent", 1, null, null, 1);
        NhanhCategoryDTO child = new NhanhCategoryDTO(11L, 10L, "C2", "Child", 2, null, null, 1);
        NhanhCategoryDTO missingId = new NhanhCategoryDTO(null, null, "C3", "Missing", 3, null, null, 1);

        List<NhanhResponse<List<NhanhCategoryDTO>>> pages = new ArrayList<>();
        pages.add(new NhanhResponse<>(1, Arrays.asList(parent, null, missingId), new NhanhResponse.Paginator("cursor-2")));
        pages.add(new NhanhResponse<>(1, List.of(child), null));

        List<Object> cursors = new ArrayList<>();
        Function<Object, NhanhResponse<List<NhanhCategoryDTO>>> fetcher = cursor -> {
            cursors.add(cursor);
            return pages.get(cursors.size() - 1);
        };

        List<NhanhCategoryDTO> result = ReflectionTestUtils.invokeMethod(categorySyncService, "fetchAllCategoriesWithFetcher", fetcher);

        assertEquals(4, result.size());
        assertSame(parent, result.get(0));
        assertNull(result.get(1));
        assertSame(missingId, result.get(2));
        assertSame(child, result.get(3));
        assertNull(cursors.get(0));
        assertEquals("cursor-2", cursors.get(1));
    }

    @Test
    @DisplayName("Should stop pagination when category response is null")
    void testFetchAllCategoriesNullResponseReturnsEmpty() {
        List<NhanhCategoryDTO> result = ReflectionTestUtils.invokeMethod(
                categorySyncService, "fetchAllCategoriesWithFetcher", (Function<Object, NhanhResponse<List<NhanhCategoryDTO>>>) cursor -> null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should stop pagination when category response code is not success")
    void testFetchAllCategoriesErrorCodeReturnsEmpty() {
        List<NhanhCategoryDTO> result = ReflectionTestUtils.invokeMethod(
                categorySyncService, "fetchAllCategoriesWithFetcher",
                (Function<Object, NhanhResponse<List<NhanhCategoryDTO>>>) cursor -> new NhanhResponse<>(0, null, null));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

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
