package com.vn.sodu.product.category.service;

import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.category.dto.CategoryDTO;
import com.vn.sodu.product.category.dto.CategoryListItemDTO;
import com.vn.sodu.product.category.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryMapper categoryMapper;
    private final CategoryRepo categoryRepo;

    public List<CategoryListItemDTO>getAll() {
        return categoryRepo.findAll()
                .stream()
                .map(categoryMapper::toListDTO)
                .toList();
    }
}
