package com.vn.sodu.product.brand.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import com.vn.sodu.product.dto.NhanhResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandSyncServiceTest {

    @Mock
    private BrandRepo brandRepo;
    @Mock
    private BrandMapper brandMapper;
    @Mock
    private NhanhService nhanhService;

    @InjectMocks
    private BrandSyncService brandSyncService;

    @Test
    @DisplayName("Should build brand URL from base URL, client id, and business id")
    void testBuildBrandListUrl() {
        ReflectionTestUtils.setField(brandSyncService, "nhanhBaseUrl", "https://pos.open.nhanh.vn/api");
        ReflectionTestUtils.setField(brandSyncService, "clientId", "77323");
        ReflectionTestUtils.setField(brandSyncService, "businessId", "224003");

        String url = brandSyncService.buildBrandListUrl();

        assertEquals("https://pos.open.nhanh.vn/v3.0/product/brand?appId=77323&businessId=224003", url);
    }

    @Test
    @DisplayName("Should build brand request body with paginator")
    void testBuildBrandListRequestBody() {
        Map<String, Object> body = brandSyncService.buildBrandListRequestBody(null);

        assertTrue(body.containsKey("paginator"));
        Map<String, Object> paginator = (Map<String, Object>) body.get("paginator");
        assertEquals(50, paginator.get("size"));
        assertFalse(paginator.containsKey("next"));
    }

    @Test
    @DisplayName("Should collect brands across next cursors and skip null items")
    void testFetchAllBrandsMultiPage() {
        NhanhBrandDTO valid1 = new NhanhBrandDTO(1L, null, "B1", "Brand 1", 1, 1L);
        NhanhBrandDTO valid2 = new NhanhBrandDTO(2L, null, "B2", "Brand 2", 1, 2L);
        NhanhBrandDTO missingId = new NhanhBrandDTO(null, null, "B3", "Brand 3", 1, 3L);

        List<NhanhResponse<List<NhanhBrandDTO>>> pages = new ArrayList<>();
        pages.add(new NhanhResponse<>(1, Arrays.asList(valid1, null, missingId), new NhanhResponse.Paginator("cursor-2")));
        pages.add(new NhanhResponse<>(1, List.of(valid2), null));

        List<Object> cursors = new ArrayList<>();
        Function<Object, NhanhResponse<List<NhanhBrandDTO>>> fetcher = cursor -> {
            cursors.add(cursor);
            return pages.get(cursors.size() - 1);
        };

        List<NhanhBrandDTO> result = brandSyncService.fetchAllBrandsWithFetcher(fetcher);

        assertEquals(2, result.size());
        assertNull(cursors.get(0));
        assertEquals("cursor-2", cursors.get(1));
    }

    @Test
    @DisplayName("Should throw when brand response is null")
    void testFetchAllBrandsNullResponseThrows() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> brandSyncService.fetchAllBrandsWithFetcher(cursor -> null));

        assertTrue(ex.getMessage().toLowerCase().contains("invalid response"));
    }

    @Test
    @DisplayName("Should throw when brand response code is not success")
    void testFetchAllBrandsErrorCodeThrows() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> brandSyncService.fetchAllBrandsWithFetcher(cursor -> new NhanhResponse<>(0, null, null)));

        assertTrue(ex.getMessage().toLowerCase().contains("nhanh api error"));
    }

    @Test
    @DisplayName("Should skip sync when brand dto has null id")
    void testSyncOneSkipsNullId() {
        boolean synced = brandSyncService.syncOne(new NhanhBrandDTO(null, null, "B", "Brand", 1, 1L));

        assertFalse(synced);
        verifyNoInteractions(brandMapper, brandRepo);
    }

    @Test
    @DisplayName("Should skip sync when mapper returns null")
    void testSyncOneSkipsWhenMapperNull() {
        NhanhBrandDTO dto = new NhanhBrandDTO(1L, null, "B", "Brand", 1, 1L);
        when(brandMapper.toEntity(dto)).thenReturn(null);

        boolean synced = brandSyncService.syncOne(dto);

        assertFalse(synced);
        verify(brandMapper).toEntity(dto);
        verifyNoInteractions(brandRepo);
    }

    @Test
    @DisplayName("Should save mapped brand")
    void testSyncOneSavesBrand() {
        NhanhBrandDTO dto = new NhanhBrandDTO(1L, null, "B", "Brand", 1, 1L);
        Brand brand = Brand.builder().id(1L).name("Brand").build();
        when(brandMapper.toEntity(dto)).thenReturn(brand);

        boolean synced = brandSyncService.syncOne(dto);

        assertTrue(synced);
        verify(brandRepo).save(brand);
    }
}
