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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
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

    /**
     * Khởi chạy tiến trình đồng bộ danh mục
     */
    public void syncCategories() {
        List<NhanhCategoryDTO> categories = fetchAllCategories();

        if (categories.isEmpty()) {
            log.info("Nhanh sync skipped: no categories found.");
            return;
        }

        // Tạo map để tra cứu nhanh thông tin cha khi đệ quy
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
                // Sử dụng đệ quy để đảm bảo thứ tự lưu: Cha trước, Con sau
                if (syncWithDependencies(dto, categoryById, synced, visiting)) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception ex) {
                failed++;
                log.error("Sync failed for categoryId={}", dto.getId(), ex);
            }
        }

        log.info("Nhanh sync done. total={}, success={}, failed={}", categories.size(), success, failed);
    }

    /**
     * Đồng bộ một danh mục đơn lẻ vào Database
     */
    @Transactional
    public boolean syncOne(NhanhCategoryDTO dto) {
        if (dto == null || dto.getId() == null) return false;

        Category category = categoryMapper.toEntity(dto);
        if (category == null) return false;

        // The FK is persisted from parentId, not from the read-only parent relation.
        Long parentId = dto.getParentId();
        category.setParentId(parentId != null && parentId > 0 ? parentId : null);
        category.setParent(null);

        try {
            categoryRepo.save(category);
            return true;
        } catch (Exception e) {
            log.error("Database error saving category {}: {}", dto.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Giải thuật DFS để giải quyết phụ thuộc cha-con
     */
    private boolean syncWithDependencies(NhanhCategoryDTO dto, Map<Long, NhanhCategoryDTO> categoryById,
                                         Set<Long> synced, Set<Long> visiting) {
        Long categoryId = dto.getId();
        if (synced.contains(categoryId)) return true;

        if (!visiting.add(categoryId)) {
            log.warn("Category cycle detected at id={}", categoryId);
            return false;
        }

        try {
            Long parentId = dto.getParentId();
            // Nếu có cha (Id > 0) và cha chưa được lưu, thì lưu cha trước
            if (parentId != null && parentId > 0 && !synced.contains(parentId)) {
                NhanhCategoryDTO parentDto = categoryById.get(parentId);
                if (parentDto != null) {
                    if (!syncWithDependencies(parentDto, categoryById, synced, visiting)) return false;
                }
            }

            if (syncOne(dto)) {
                synced.add(categoryId);
                return true;
            }
            return false;
        } finally {
            visiting.remove(categoryId);
        }
    }

    /**
     * Lấy toàn bộ danh mục từ API Nhanh.vn
     */
    public List<NhanhCategoryDTO> fetchAllCategories() {
        String url = buildCategoryListUrl();
        String accessToken = nhanhService.getValidAccessToken();

        return fetchAllCategoriesWithFetcher(nextCursor -> {
            Map<String, Object> body = buildCategoryListRequestBody(nextCursor);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            Supplier<NhanhResponse<List<NhanhCategoryDTO>>> call = () -> {
                ResponseEntity<NhanhResponse<List<NhanhCategoryDTO>>> response =
                        restTemplate.exchange(url, HttpMethod.POST, request,
                                new ParameterizedTypeReference<NhanhResponse<List<NhanhCategoryDTO>>>() {});
                return response.getBody();
            };

            return withRetry(call, 3, 500);
        });
    }

    // --- Các hàm hỗ trợ build URL/Request (Giữ nguyên logic của bạn) ---
    private String buildCategoryListUrl() {
        return UriComponentsBuilder.fromHttpUrl(nhanhBaseUrl).replacePath(CATEGORY_LIST_PATH)
                .queryParam("appId", clientId).queryParam("businessId", businessId).toUriString();
    }

    private Map<String, Object> buildCategoryListRequestBody(Object nextCursor) {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> paginator = new HashMap<>();
        paginator.put("size", 50);
        if (nextCursor != null) paginator.put("next", nextCursor);
        body.put("paginator", paginator);
        return body;
    }

    private List<NhanhCategoryDTO> fetchAllCategoriesWithFetcher(java.util.function.Function<Object, NhanhResponse<List<NhanhCategoryDTO>>> fetcher) {
        List<NhanhCategoryDTO> result = new ArrayList<>();
        Object nextCursor = null;

        while (true) {
            NhanhResponse<List<NhanhCategoryDTO>> resp = fetcher.apply(nextCursor);
            if (resp == null || resp.getCode() != 1 || resp.getData() == null) break;

            result.addAll(resp.getData());
            if (resp.getPaginator() == null || resp.getPaginator().getNext() == null) break;
            nextCursor = resp.getPaginator().getNext();
        }
        return result;
    }

    private <T> T withRetry(Supplier<T> supplier, int maxAttempts, long initialDelayMs) {
        int attempt = 0; long delay = initialDelayMs;
        while (true) {
            try { return supplier.get(); } catch (Exception ex) {
                if (++attempt >= maxAttempts) throw ex;
                try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new RuntimeException(ie); }
                delay *= 2;
            }
        }
    }
}
