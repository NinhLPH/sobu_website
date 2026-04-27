package com.vn.sodu.product.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.dto.NhanhResponse;
import com.vn.sodu.product.mapper.ProductMapper;
import com.vn.sodu.product.repo.ProductAttributeRepo;
import com.vn.sodu.product.repo.ProductImageRepo;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.repo.ProductUnitRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSyncService {

    private final ProductRepo productRepo;
    private final ProductUnitRepo productUnitRepo;
    private final ProductAttributeRepo productAttributeRepo;
    private final ProductImageRepo productImageRepo;
    private final ProductMapper productMapper;
    private final NhanhService nhanhService;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String PRODUCT_LIST_PATH = "/v3.0/product/list";

    @Value("${nhanh.base-url:}")
    private String nhanhBaseUrl;

    @Value("${nhanh.client-id:}")
    private String clientId;

    @Value("${nhanh.business-id:}")
    private String businessId;

    @Scheduled(cron = "${nhanh.sync.cron:0 0 * * * *}")
    public void syncProducts() {
        // Phase 5: Get last sync time for incremental sync
        com.vn.sodu.nhanh.NhanhIntegration integration = nhanhService.getIntegration().orElse(null);
        Long lastSyncTime = (integration != null) ? integration.getLastProductSyncTime() : null;
        
        // Record current time to be the next lastSyncTime (in seconds)
        long currentSyncTime = java.time.Instant.now().getEpochSecond();

        List<NhanhProductDTO> products = fetchAllProducts(lastSyncTime);

        if (products.isEmpty()) {
            log.info("Nhanh sync skipped: no products found since {}", lastSyncTime);
            // Even if no products, we update the lastSyncTime to now to move the window forward
            nhanhService.updateLastSyncTime(currentSyncTime);
            return;
        }

        int success = 0;
        int failed = 0;

        for (NhanhProductDTO dto : products) {
            try {
                syncOne(dto);
                success++;
            } catch (Exception ex) {
                failed++;
                log.error("Sync failed for productId={}", dto.getId(), ex);
            }
        }

        log.info("Nhanh sync done. total={}, success={}, failed={}",
                products.size(), success, failed);
        
        // Update lastSyncTime after successful sync
        nhanhService.updateLastSyncTime(currentSyncTime);
    }

    // =============================
    // SYNC 1 PRODUCT (transaction riêng)
    // =============================
    @Transactional
    public void syncOne(NhanhProductDTO dto) {
        if (dto == null || dto.getId() == null) {
            return;
        }

        log.info("Syncing product externalId={}", dto.getId());

        // Mapping layer
        Product product = mapProduct(dto);

        // preserve previous behavior: if mapper returns null, surface NPE to callers/tests
        if (product == null) {
            throw new NullPointerException("Mapper returned null");
        }

        // Save layer (transactional)
        saveProduct(product, dto);

        log.info("Synced product id={} externalId={}", product.getId(), dto.getId());
    }

    // Mapping: convert DTO to Product entity and set external id
    Product mapProduct(NhanhProductDTO dto) {
        Product product = productMapper.toEntity(dto);
        if (product == null) return null;
        product.setExternalId(dto.getId());

        // If existing, keep PK
        java.util.Optional<Product> existing = productRepo.findByExternalId(dto.getId());
        if (existing.isPresent()) {
            product.setId(existing.get().getId());
        } else {
            product.setId(dto.getId());
        }
        return product;
    }

    // Save: persist product and related children (units, attributes, images)
    void saveProduct(Product product, NhanhProductDTO dto) {
        if (product == null) return;

        productRepo.save(product);
        Long productId = product.getId();

        // delete old children
        productUnitRepo.deleteByProductId(productId);
        productAttributeRepo.deleteByProductId(productId);
        productImageRepo.deleteByProductId(productId);

        // map children
        List<ProductUnit> units = productMapper.toUnits(productId, dto);
        List<ProductAttribute> attributes = productMapper.toAttributes(productId, dto);
        List<ProductImage> images = productMapper.toImages(productId, dto);

        if (units != null && !units.isEmpty()) {
            saveInBatches(productUnitRepo::saveAll, units);
        }
        if (attributes != null && !attributes.isEmpty()) {
            saveInBatches(productAttributeRepo::saveAll, attributes);
        }
        if (images != null && !images.isEmpty()) {
            saveInBatches(productImageRepo::saveAll, images);
        }
    }

    // Generic batch saver (chunk size tuned for performance)
    private <T> void saveInBatches(java.util.function.Consumer<java.util.List<T>> saver, java.util.List<T> items) {
        final int batchSize = 100;
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(items.size(), i + batchSize);
            java.util.List<T> chunk = items.subList(i, end);
            saver.accept(chunk);
        }
    }

    // =============================
    // FETCH ALL (pagination)
    // =============================
    public List<NhanhProductDTO> fetchAllProducts(Long lastSyncTime) {

        String url = buildProductListUrl();

        String accessToken = nhanhService.getValidAccessToken();


        return fetchAllProductsWithFetcher(nextCursor -> {
            Map<String, Object> body = buildProductListRequestBody(lastSyncTime, nextCursor);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);
            log.info(url);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // Kiểu trả về phải là NhanhProductListResponse
            Supplier<NhanhResponse<List<NhanhProductDTO>>> call = () -> {
                ResponseEntity<String> raw = restTemplate.postForEntity(url, request, String.class);
                log.info("Dữ liệu thô từ Nhanh: {}", raw.getBody());
                ResponseEntity<NhanhResponse<List<NhanhProductDTO>>> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                request,
                                new ParameterizedTypeReference<NhanhResponse<List<NhanhProductDTO>>>() {}
                        );
                return response.getBody();
            };

            return withRetry(call, 3, 500);
        });
    }

    String buildProductListUrl() {
        if (nhanhBaseUrl == null || nhanhBaseUrl.isBlank()) {
            throw new IllegalStateException("nhanh.base-url is not configured");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("nhanh.client-id is not configured");
        }
        if (businessId == null || businessId.isBlank()) {
            throw new IllegalStateException("nhanh.business-id is not configured");
        }

        return UriComponentsBuilder.fromHttpUrl(nhanhBaseUrl)
                .replacePath(PRODUCT_LIST_PATH)
                .queryParam("appId", clientId)
                .queryParam("businessId", businessId)
                .toUriString();
    }

    Map<String, Object> buildProductListRequestBody(Long lastSyncTime, Object nextCursor) {
        Map<String, Object> body = new HashMap<>();
        Long normalizedLastSyncTime = normalizeLastSyncTime(lastSyncTime);

        if (normalizedLastSyncTime != null) {
            Map<String, Object> filters = new HashMap<>();
            filters.put("updatedAtFrom", normalizedLastSyncTime);
            body.put("filters", filters);
        }

        Map<String, Object> paginator = new HashMap<>();
        paginator.put("size", 50);
        if (nextCursor != null) {
            paginator.put("next", nextCursor);
        }
        body.put("paginator", paginator);

        return body;
    }

    private Long normalizeLastSyncTime(Long lastSyncTime) {
        if (lastSyncTime == null || lastSyncTime <= 0) {
            return null;
        }
        return lastSyncTime;
    }

    // Package-private for testing: supply a fetcher function that returns NhanhProductListResponse for a page
    List<NhanhProductDTO> fetchAllProductsWithFetcher(
            java.util.function.Function<Object, NhanhResponse<List<NhanhProductDTO>>> fetcher) {

        List<NhanhProductDTO> result = new java.util.ArrayList<>();
        Object nextCursor = null;
        int requestCount = 0;

        while (true) {
            requestCount++;
            NhanhResponse<List<NhanhProductDTO>> resp;
            try {
                resp = fetcher.apply(nextCursor);
            } catch (Exception ex) {
                log.error("Failed to fetch products from Nhanh at request={}", requestCount, ex);
                throw new RuntimeException("Nhanh API fetch failed", ex);
            }

            if (resp == null) {
                log.error("Nhanh response is null at request={}", requestCount);
                throw new RuntimeException("Invalid response: null");
            }

            if (resp.getCode() != 1) {
                log.error("Nhanh API returned non-success code at request={}: {}", requestCount, resp.getCode());
                throw new RuntimeException("Nhanh API error, code=" + resp.getCode());
            }

            if (resp.getData() == null) {
                log.error("Nhanh response data is null at request={}", requestCount);
                throw new RuntimeException("Invalid response: data is null");
            }

            List<NhanhProductDTO> products = resp.getData();
            if (products.isEmpty()) {
                break;
            }

            result.addAll(products);

            if (resp.getPaginator() == null || resp.getPaginator().getNext() == null) {
                break;
            }

            nextCursor = resp.getPaginator().getNext();
        }
        return result;
    }

    // Simple retry helper with exponential backoff
    private <T> T withRetry(java.util.function.Supplier<T> supplier, int maxAttempts, long initialDelayMs) {
        int attempt = 0;
        long delay = initialDelayMs;
        while (true) {
            try {
                attempt++;
                return supplier.get();
            } catch (Exception ex) {
                if (attempt >= maxAttempts) {
                    log.error("Operation failed after {} attempts", attempt, ex);
                    throw ex;
                }
                log.warn("Operation failed on attempt {} - retrying after {}ms", attempt, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                delay *= 2;
            }
        }
    }
}
