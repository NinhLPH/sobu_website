package com.vn.sodu.voucher.service;

import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CategoryHierarchyService {

    private final CategoryRepo categoryRepo;

    // Cache category tree mapping parentId -> List<childId>
    private final Map<Long, Set<Long>> parentToChildrenMap = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public synchronized void reloadHierarchy() {
        parentToChildrenMap.clear();
        List<Category> allCategories = categoryRepo.findAll();
        for (Category cat : allCategories) {
            if (cat.getParentId() != null) {
                parentToChildrenMap.computeIfAbsent(cat.getParentId(), k -> new HashSet<>()).add(cat.getId());
            }
        }
        initialized = true;
    }

    private void ensureInitialized() {
        if (!initialized) {
            reloadHierarchy();
        }
    }

    /**
     * Given a set of category IDs, returns a set containing all those IDs plus all their descendant IDs.
     */
    public Set<Long> expandDescendantCategoryIds(Set<Long> rootCategoryIds) {
        if (rootCategoryIds == null || rootCategoryIds.isEmpty()) {
            return Collections.emptySet();
        }

        ensureInitialized();
        Set<Long> result = new HashSet<>(rootCategoryIds);
        Queue<Long> queue = new LinkedList<>(rootCategoryIds);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            Set<Long> children = parentToChildrenMap.get(current);
            if (children != null) {
                for (Long childId : children) {
                    if (result.add(childId)) {
                        queue.add(childId);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Check if a product's categoryId is within the target categories or their descendants.
     */
    public boolean isCategoryEligible(Long productCategoryId, Set<Long> targetCategoryIds) {
        if (productCategoryId == null || targetCategoryIds == null || targetCategoryIds.isEmpty()) {
            return false;
        }
        Set<Long> expanded = expandDescendantCategoryIds(targetCategoryIds);
        return expanded.contains(productCategoryId);
    }
}
