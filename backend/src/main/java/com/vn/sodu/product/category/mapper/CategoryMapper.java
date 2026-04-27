package com.vn.sodu.product.category.mapper;

import com.vn.sodu.product.category.Category;
import com.vn.sodu.product.category.dto.CategoryDTO;
import com.vn.sodu.product.category.dto.CategoryListItemDTO;
import com.vn.sodu.product.category.dto.NhanhCategoryDTO;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    /**
     * Convert Category entity to CategoryDTO
     */
    public CategoryDTO toDTO(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryDTO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .code(category.getCode())
                .name(category.getName())
                .order(category.getOrder())
                .image(category.getImage())
                .content(category.getContent())
                .status(category.getStatus())
                .build();
    }

    /**
     * Convert CategoryDTO to Category entity
     */
    public Category toEntity(CategoryDTO dto) {
        if (dto == null) {
            return null;
        }
        return Category.builder()
                .id(dto.getId())
                .parentId(dto.getParentId())
                .code(dto.getCode())
                .name(dto.getName())
                .order(dto.getOrder())
                .image(dto.getImage())
                .content(dto.getContent())
                .status(dto.getStatus())
                .build();
    }

    /**
     * Convert NhanhCategoryDTO to Category entity
     */
    public Category toEntity(NhanhCategoryDTO dto) {
        if (dto == null) {
            return null;
        }
        return Category.builder()
                .id(dto.getId())
                .parentId(dto.getParentId())
                .code(dto.getCode())
                .name(dto.getName())
                .order(dto.getOrder())
                .image(dto.getImage())
                .content(dto.getContent())
                .status(dto.getStatus())
                .build();
    }

    /**
     * Convert Category entity to CategoryListDTO
     */
    public CategoryListItemDTO toListDTO(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryListItemDTO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .code(category.getCode())
                .name(category.getName())
                .order(category.getOrder())
                .image(category.getImage())
                .status(category.getStatus())
                .build();
    }

}
