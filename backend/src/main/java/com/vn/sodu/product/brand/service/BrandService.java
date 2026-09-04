package com.vn.sodu.product.brand.service;

import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.BrandDTO;
import com.vn.sodu.product.brand.dto.BrandListItemDTO;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import com.vn.sodu.seo.SlugHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepo brandRepo;
    private final BrandMapper brandMapper;
    private final SlugHistoryService slugHistoryService;

    @Transactional(readOnly = true)
    public List<BrandListItemDTO> getAll() {
        return brandRepo.findAll()
                .stream()
                .map(brandMapper::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandDTO getBySlug(String slugOrId) {
        if (slugOrId == null || slugOrId.isBlank()) {
            throw new NotFoundException("Slug hoặc ID thương hiệu không hợp lệ");
        }

        String input = slugOrId.trim();

        // 1. Find by slug directly
        Optional<Brand> brandOpt = brandRepo.findBySlug(input);

        // 2. Check slug history
        if (brandOpt.isEmpty()) {
            Optional<String> currentSlugOpt = slugHistoryService.findCurrentSlug("BRAND", input);
            if (currentSlugOpt.isPresent()) {
                brandOpt = brandRepo.findBySlug(currentSlugOpt.get());
            }
        }

        // 3. Fallback to ID
        if (brandOpt.isEmpty() && input.matches("^\\d+$")) {
            try {
                long id = Long.parseLong(input);
                brandOpt = brandRepo.findById(id);
            } catch (NumberFormatException ignored) {
            }
        }

        Brand brand = brandOpt.orElseThrow(() ->
                new NotFoundException("Không tìm thấy thương hiệu với slug/ID: " + slugOrId));

        return brandMapper.toDTO(brand);
    }
}
