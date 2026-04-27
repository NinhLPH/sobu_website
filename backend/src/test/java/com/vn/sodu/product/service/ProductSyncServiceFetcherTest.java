package com.vn.sodu.product.service;

import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.dto.NhanhResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSyncService Fetcher Tests")
public class ProductSyncServiceFetcherTest {

    @Mock
    private com.vn.sodu.product.repo.ProductRepo productRepo;
    @Mock
    private com.vn.sodu.product.repo.ProductUnitRepo productUnitRepo;
    @Mock
    private com.vn.sodu.product.repo.ProductAttributeRepo productAttributeRepo;
    @Mock
    private com.vn.sodu.product.repo.ProductImageRepo productImageRepo;
    @Mock
    private com.vn.sodu.product.mapper.ProductMapper productMapper;
    @Mock
    private com.vn.sodu.nhanh.service.NhanhService nhanhService;

    @InjectMocks
    private ProductSyncService productSyncService;

    @Test
    @DisplayName("Should collect products across multiple next cursors")
    void testFetchAllProductsMultiPage() {
        List<NhanhResponse<List<NhanhProductDTO>>> pages = new ArrayList<>();

        NhanhProductDTO first = new NhanhProductDTO();
        first.setId(11L);
        NhanhProductDTO second = new NhanhProductDTO();
        second.setId(12L);
        pages.add(new NhanhResponse<>(1, Arrays.asList(first, second), new NhanhResponse.Paginator("cursor-2")));

        NhanhProductDTO third = new NhanhProductDTO();
        third.setId(21L);
        NhanhProductDTO fourth = new NhanhProductDTO();
        fourth.setId(22L);
        pages.add(new NhanhResponse<>(1, Arrays.asList(third, fourth), null));

        List<Object> cursors = new ArrayList<>();
        Function<Object, NhanhResponse<List<NhanhProductDTO>>> fetcher = cursor -> {
            cursors.add(cursor);
            return pages.get(cursors.size() - 1);
        };

        List<NhanhProductDTO> result = productSyncService.fetchAllProductsWithFetcher(fetcher);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertNull(cursors.get(0));
        assertEquals("cursor-2", cursors.get(1));
        assertTrue(result.stream().anyMatch(x -> x.getId() != null && x.getId() == 11L));
        assertTrue(result.stream().anyMatch(x -> x.getId() != null && x.getId() == 22L));
    }

    @Test
    @DisplayName("Should throw when API returns non-success code")
    void testFetchAllProductsNonSuccessCodeThrows() {
        Function<Object, NhanhResponse<List<NhanhProductDTO>>> fetcher =
                cursor -> new NhanhResponse<>(0, null, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productSyncService.fetchAllProductsWithFetcher(fetcher));

        assertTrue(ex.getMessage().toLowerCase().contains("nhanh api error"));
    }

    @Test
    @DisplayName("Should throw when response is null")
    void testFetchAllProductsNullResponseThrows() {
        Function<Object, NhanhResponse<List<NhanhProductDTO>>> fetcher = cursor -> null;

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productSyncService.fetchAllProductsWithFetcher(fetcher));

        assertTrue(ex.getMessage().toLowerCase().contains("invalid response"));
    }
}
