package com.vn.sodu.product.brand.service;

import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.BrandListItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepo brandRepo;

    public List<BrandListItemDTO> getAll() {
        return brandRepo.findAll()
                .stream()
                .map(brand -> BrandListItemDTO.builder()
                        .id(brand.getId())
                        .parentId(brand.getParentId())
                        .code(brand.getCode())
                        .name(brand.getName())
                        .status(brand.getStatus())
                        .build())
                .toList();
    }
}
