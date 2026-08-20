package com.vn.sodu.product.category.service;

import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.audit.AuditService;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.category.dto.CategoryDTO;
import com.vn.sodu.product.category.dto.CategoryListItemDTO;
import com.vn.sodu.product.category.dto.CategoryRequest;
import com.vn.sodu.product.category.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepo categoryRepo;
    private final CategoryMapper categoryMapper;
    private final ProductRepo productRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CategoryListItemDTO> getAllCategories() {
        return categoryRepo.findAll()
                .stream()
                .map(categoryMapper::toListDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
        return categoryMapper.toDTO(category);
    }

    @Transactional
    public CategoryDTO createCategory(CategoryRequest request) {
        validateRequest(request);

        Category category = Category.builder()
                .code(request.getCode())
                .name(request.getName())
                .parentId(request.getParentId())
                .order(request.getOrder())
                .image(request.getImage())
                .content(request.getContent())
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .build();

        category = categoryRepo.save(category);

        auditService.record(AuditAction.CATALOG_MUTATION, "CATEGORY", category.getId().toString(),
                null, toJson(category), "Category created");

        return categoryMapper.toDTO(category);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryRequest request) {
        validateRequest(request);

        Category category = categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        String beforeJson = toJson(category);

        category.setCode(request.getCode());
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setOrder(request.getOrder());
        category.setImage(request.getImage());
        category.setContent(request.getContent());
        category.setStatus(request.getStatus() != null ? request.getStatus() : category.getStatus());

        category = categoryRepo.save(category);

        auditService.record(AuditAction.CATALOG_MUTATION, "CATEGORY", id.toString(),
                beforeJson, toJson(category), "Category updated");

        return categoryMapper.toDTO(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        // Guard: reject if category has children
        if (categoryRepo.existsByParentId(id)) {
            throw new BadRequestException("Cannot delete category with child categories. Deactivate instead.");
        }

        // Guard: reject if products reference this category
        if (productRepo.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete category referenced by products. Deactivate instead.");
        }

        String beforeJson = toJson(category);
        categoryRepo.delete(category);

        auditService.record(AuditAction.CATALOG_MUTATION, "CATEGORY", id.toString(),
                beforeJson, null, "Category deleted");
    }

    @Transactional
    public CategoryDTO setCategoryStatus(Long id, Integer status) {
        Category category = categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

        String beforeJson = toJson(category);
        category.setStatus(status);
        category = categoryRepo.save(category);

        auditService.record(AuditAction.CATALOG_MUTATION, "CATEGORY", id.toString(),
                beforeJson, toJson(category), "Category status changed to " + status);

        return categoryMapper.toDTO(category);
    }

    private void validateRequest(CategoryRequest request) {
        if (request == null) {
            throw new BadRequestException("Category payload is required");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Category code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        if (request.getParentId() != null && request.getParentId().equals(0L)) {
            request.setParentId(null);
        }
    }

    private String toJson(Category category) {
        if (category == null) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(category);
        } catch (Exception e) {
            return "{}";
        }
    }
}