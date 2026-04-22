package com.vn.sodu.product.category.service;

import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.category.dto.NhanhCategoryDTO;
import com.vn.sodu.product.category.dto.NhanhCategoryListData;
import com.vn.sodu.product.category.mapper.CategoryMapper;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
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

        List<NhanhCategoryDTO> result = new ArrayList<>();
        String url = nhanhCategoriesUrl;

        while (url != null && !url.isBlank()) {
            try {
                ResponseEntity<NhanhResponse<NhanhCategoryListData>> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                null,
                                new ParameterizedTypeReference<NhanhResponse<NhanhCategoryListData>>() {
                                }
                        );

                NhanhResponse<NhanhCategoryListData> body = response.getBody();
                if (body == null || body.getData() == null || body.getData().getCategories() == null) {
                    break;
                }

                result.addAll(body.getData().getCategories());

                if (body.getPaginator() != null) {
                    url = body.getPaginator().getNext();
                } else {
                    url = null;
                }
            } catch (RestClientException ex) {
                log.error("Failed to fetch categories from Nhanh API. url={}", url, ex);
                throw new RuntimeException("Nhanh API fetch failed", ex);
            }
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

            syncOne(dto);
            synced.add(categoryId);
            return true;
        } finally {
            visiting.remove(categoryId);
        }
    }
}
