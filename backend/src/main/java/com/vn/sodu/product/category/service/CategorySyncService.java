package com.vn.sodu.product.category.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.category.dto.NhanhCategoryDTO;
import com.vn.sodu.product.category.mapper.CategoryMapper;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategorySyncService {

    private final CategoryRepo categoryRepo;
    private final CategoryMapper categoryMapper;
    private final NhanhService nhanhService;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String CATEGORY_LIST_PATH = "/v3.0/product/category";

    @Value("${nhanh.base-url:}")
    private String nhanhBaseUrl;

    @Value("${nhanh.client-id:}")
    private String clientId;

    @Value("${nhanh.business-id:}")
    private String businessId;

    public void syncCategories() {
        List<NhanhCategoryDTO> categories = fetchAllCategories();

        if (categories.isEmpty()) {
            log.info("Nhanh sync skipped: no categories");
            return;
        }


        Map<Long, NhanhCategoryDTO> categoryById = new HashMap<>();
        for (NhanhCategoryDTO dto : categories) {
            if (dto != null && dto.getId() != null) {
                categoryById.put(dto.getId(), dto);
            }
        }

        int success = 0;
        int failed = 0;
        Set<Long> synced = new HashSet<>();
        Set<Long> visiting = new HashSet<>();

        for (NhanhCategoryDTO dto : categories) {
            try {
                if (syncWithDependencies(dto, categoryById, synced, visiting)) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception ex) {
                failed++;
                log.error("Sync failed for categoryId={}", dto != null ? dto.getId() : null, ex);
            }
        }

        log.info("Nhanh sync done. total={}, success={}, failed={}",
                categories.size(), success, failed);
    }

    @Transactional
    public boolean syncOne(NhanhCategoryDTO dto) {
        if (dto == null || dto.getId() == null) {
            log.warn("Skipping category sync item with null payload or id");
            return false;
        }

        Category category = categoryMapper.toEntity(dto);
        if (category == null) {
            log.warn("Skipping category sync item because mapper returned null for id={}", dto.getId());
            return false;
        }
        categoryRepo.save(category);
        return true;
    }

    public List<NhanhCategoryDTO> fetchAllCategories() {
        String url = buildCategoryListUrl();
        String accessToken = nhanhService.getValidAccessToken();

        return fetchAllCategoriesWithFetcher(nextCursor -> {
            Map<String, Object> body = buildCategoryListRequestBody(nextCursor);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);

            log.info(url);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            Supplier<NhanhResponse<List<NhanhCategoryDTO>>> call = () -> {
                ResponseEntity<String> raw = restTemplate.postForEntity(url, request, String.class);
                log.info("Dữ liệu thô từ Nhanh: {}", raw.getBody());
                ResponseEntity<NhanhResponse<List<NhanhCategoryDTO>>> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                request,
                                new ParameterizedTypeReference<NhanhResponse<List<NhanhCategoryDTO>>>() {}
                        );
                return response.getBody();
            };

            return withRetry(call, 3, 500);
        });
    }

    String buildCategoryListUrl() {
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
                .replacePath(CATEGORY_LIST_PATH)
                .queryParam("appId", clientId)
                .queryParam("businessId", businessId)
                .toUriString();
    }

    Map<String, Object> buildCategoryListRequestBody(Object nextCursor) {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> paginator = new HashMap<>();
        paginator.put("size", 50);
        if (nextCursor != null) {
            paginator.put("next", nextCursor);
        }
        body.put("paginator", paginator);
        return body;
    }

    List<NhanhCategoryDTO> fetchAllCategoriesWithFetcher(java.util.function.Function<Object, NhanhResponse<List<NhanhCategoryDTO>>> fetcher) {
        List<NhanhCategoryDTO> result = new ArrayList<>();
        Object nextCursor = null;
        int requestCount = 0;

        while (true) {
            requestCount++;
            NhanhResponse<List<NhanhCategoryDTO>> resp;
            try {
                resp = fetcher.apply(nextCursor);
            } catch (Exception ex) {
                log.error("Failed to fetch categories at request={}", requestCount, ex);
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

            for (NhanhCategoryDTO dto : resp.getData()) {
                if (dto == null || dto.getId() == null) {
                    log.warn("Skipping category payload item with null dto or id");
                    continue;
                }
                result.add(dto);
            }

            if (resp.getPaginator() == null || resp.getPaginator().getNext() == null) {
                break;
            }

            nextCursor = resp.getPaginator().getNext();
        }

        return result;
    }

    private boolean syncWithDependencies(
            NhanhCategoryDTO dto,
            Map<Long, NhanhCategoryDTO> categoryById,
            Set<Long> synced,
            Set<Long> visiting
    ) {
        if (dto == null || dto.getId() == null) {
            return false;
        }

        Long categoryId = dto.getId();
        if (synced.contains(categoryId)) {
            return true;
        }

        if (!visiting.add(categoryId)) {
            log.warn("Category cycle detected for categoryId={}", categoryId);
            return false;
        }

        try {
            Long parentId = dto.getParentId();
            if (parentId != null && !synced.contains(parentId)) {
                NhanhCategoryDTO parent = categoryById.get(parentId);
                if (parent != null && !syncWithDependencies(parent, categoryById, synced, visiting)) {
                    return false;
                }
            }

            if (!syncOne(dto)) {
                return false;
            }
            synced.add(categoryId);
            return true;
        } finally {
            visiting.remove(categoryId);
        }
    }

    private <T> T withRetry(java.util.function.Supplier<T> supplier, int maxAttempts, long initialDelayMs) {
        int attempt = 0;
        long delay = initialDelayMs;

        while (true) {
            attempt++;
            try {
                return supplier.get();
            } catch (RuntimeException ex) {
                if (attempt >= maxAttempts) {
                    throw ex;
                }
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
