package com.vn.sodu.product.service;

import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.dto.NhanhProductListResponse;
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
    @DisplayName("Should collect products across multiple pages")
    void testFetchAllProductsMultiPage() {
        // Prepare pages: 3 pages, each with 2 products
        List<NhanhProductListResponse> pages = new ArrayList<>();
        for (int p = 1; p <= 3; p++) {
            NhanhProductDTO a = new NhanhProductDTO(); a.setId((long) (p * 10 + 1));
            NhanhProductDTO b = new NhanhProductDTO(); b.setId((long) (p * 10 + 2));
            List<NhanhProductDTO> list = Arrays.asList(a, b);
            NhanhProductListResponse.Data data = new NhanhProductListResponse.Data(list, p, 3);
            NhanhProductListResponse resp = new NhanhProductListResponse(1, data);
            pages.add(resp);
        }

        Function<Integer, NhanhProductListResponse> fetcher = page -> pages.get(page - 1);

        List<NhanhProductDTO> result = productSyncService.fetchAllProductsWithFetcher(fetcher);

        assertNotNull(result);
        assertEquals(6, result.size());
        // ensure ids present
        assertTrue(result.stream().anyMatch(x -> x.getId() != null && x.getId() == 11L));
        assertTrue(result.stream().anyMatch(x -> x.getId() != null && x.getId() == 32L));
    }

    @Test
    @DisplayName("Should throw when API returns non-success code")
    void testFetchAllProductsNonSuccessCodeThrows() {
        NhanhProductListResponse bad = new NhanhProductListResponse(0, null);
        Function<Integer, NhanhProductListResponse> fetcher = page -> bad;

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productSyncService.fetchAllProductsWithFetcher(fetcher));

        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        assertTrue(msg.contains("nhanh api error") || msg.contains("code") || msg.contains("invalid response") || msg.contains("null"),
                "Expected message to indicate API error or invalid/ null response");
    }

    @Test
    @DisplayName("Should throw when response is null")
    void testFetchAllProductsNullResponseThrows() {
        Function<Integer, NhanhProductListResponse> fetcher = page -> null;

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productSyncService.fetchAllProductsWithFetcher(fetcher));

        assertTrue(ex.getMessage().toLowerCase().contains("invalid response") || ex.getMessage().toLowerCase().contains("null"));
    }
}
