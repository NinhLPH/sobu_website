package com.vn.sodu.product.category.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.category.dto.NhanhCategoryDTO;
import com.vn.sodu.product.category.dto.NhanhCategoryListData;
import com.vn.sodu.product.category.dto.NhanhCategoryListResponse;
import com.vn.sodu.product.category.mapper.CategoryMapper;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategorySyncService {

    private final CategoryRepo categoryRepo;
    private final CategoryMapper categoryMapper;
    private final NhanhService nhanhService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nhanh.api.categories-url:}")
    private String nhanhCategoriesUrl;

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
    public void syncOne(NhanhCategoryDTO dto) {
        if (dto == null || dto.getId() == null) {
            return;
        }

        Category category = categoryMapper.toEntity(dto);
        categoryRepo.save(category);
    }

    public List<NhanhCategoryDTO> fetchAllCategories() {
        if (nhanhCategoriesUrl == null || nhanhCategoriesUrl.isBlank()) {
            throw new IllegalStateException("nhanh.api.categories-url is not configured");
        }

        String accessToken = nhanhService.getValidAccessToken();

        return fetchAllCategoriesWithFetcher(page -> {
            Map<String, Object> body = Map.of(
                    "page", page,
                    "pageSize", 50
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<NhanhCategoryListResponse> response =
                    restTemplate.postForEntity(nhanhCategoriesUrl, request, NhanhCategoryListResponse.class);

            return response.getBody();
        });
    }

    List<NhanhCategoryDTO> fetchAllCategoriesWithFetcher(java.util.function.Function<Integer, NhanhCategoryListResponse> fetcher) {
        List<NhanhCategoryDTO> result = new ArrayList<>();

        int page = 1;
        int totalPages = 1;

        do {
            NhanhCategoryListResponse resp;
            try {
                resp = fetcher.apply(page);
            } catch (Exception ex) {
                log.error("Failed to fetch categories for page={}", page, ex);
                throw new RuntimeException("Nhanh API fetch failed", ex);
            }

            if (resp == null) {
                log.error("Nhanh response is null for page={}", page);
                throw new RuntimeException("Invalid response: null");
            }

            if (resp.getData() == null) {
                log.error("Nhanh response data is null for page={}", page);
                throw new RuntimeException("Invalid response: data is null");
            }

            if (resp.getCode() != 1) {
                log.error("Nhanh API returned non-success code for page={}: {}", page, resp.getCode());
                throw new RuntimeException("Nhanh API error, code=" + resp.getCode());
            }

            if (resp.getData().getCategories() != null) {
                result.addAll(resp.getData().getCategories());
            } else {
                log.warn("Categories is null for page={}", page);
            }

            totalPages = resp.getData().getTotalPages();
            page++;
        } while (page <= totalPages);

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

            syncOne(dto);
            synced.add(categoryId);
            return true;
        } finally {
            visiting.remove(categoryId);
        }
    }
}
