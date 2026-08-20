package com.vn.sodu.product.category.service;

import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.category.dto.CategoryDTO;
import com.vn.sodu.product.category.dto.CategoryListItemDTO;
import com.vn.sodu.product.category.mapper.CategoryMapper;
import com.vn.sodu.seo.SlugHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryMapper categoryMapper;
    private final CategoryRepo categoryRepo;
    private final SlugHistoryService slugHistoryService;

    @Transactional(readOnly = true)
    public List<CategoryListItemDTO> getAll() {
        return categoryRepo.findAll()
                .stream()
                .map(categoryMapper::toListDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDTO getBySlug(String slugOrId) {
        if (slugOrId == null || slugOrId.isBlank()) {
            throw new NotFoundException("Slug hoặc ID danh mục không hợp lệ");
        }

        String input = slugOrId.trim();

        // 1. Find by slug directly
        Optional<Category> categoryOpt = categoryRepo.findBySlug(input);

        // 2. Check slug history
        if (categoryOpt.isEmpty()) {
            Optional<String> currentSlugOpt = slugHistoryService.findCurrentSlug("CATEGORY", input);
            if (currentSlugOpt.isPresent()) {
                categoryOpt = categoryRepo.findBySlug(currentSlugOpt.get());
            }
        }

        // 3. Fallback to ID
        if (categoryOpt.isEmpty() && input.matches("^\\d+$")) {
            try {
                long id = Long.parseLong(input);
                categoryOpt = categoryRepo.findById(id);
            } catch (NumberFormatException ignored) {
            }
        }

        Category category = categoryOpt.orElseThrow(() ->
                new NotFoundException("Không tìm thấy danh mục với slug/ID: " + slugOrId));

        return categoryMapper.toDTO(category);
    }
}
