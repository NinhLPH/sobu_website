package com.vn.sodu.product.category.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.category.dto.NhanhCategoryDTO;
import com.vn.sodu.product.category.mapper.CategoryMapper;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import com.vn.sodu.nhanh.service.NhanhClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategorySyncService {

    private final CategoryRepo categoryRepo;
    private final CategoryMapper categoryMapper;
    private final NhanhService nhanhService;
    private final NhanhClient nhanhClient;

    private static final String CATEGORY_LIST_PATH = "/v3.0/product/category";

    /**
     * Khởi chạy tiến trình đồng bộ danh mục
     */
    @Scheduled(cron = "${nhanh.sync.cron:0 0 */12 * * *}")
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
        String accessToken = nhanhService.getValidAccessToken();

        return nhanhClient.fetchAllPages(
                CATEGORY_LIST_PATH,
                accessToken,
                null,
                new org.springframework.core.ParameterizedTypeReference<NhanhResponse<List<NhanhCategoryDTO>>>() {}
        );
    }
}
