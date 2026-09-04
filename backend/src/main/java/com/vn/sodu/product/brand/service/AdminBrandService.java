package com.vn.sodu.product.brand.service;

import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.audit.AuditService;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.BrandListItemDTO;
import com.vn.sodu.product.brand.dto.BrandRequest;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminBrandService {

    private final BrandRepo brandRepo;
    private final BrandMapper brandMapper;
    private final ProductRepo productRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BrandListItemDTO> getAllBrands() {
        return brandRepo.findAll()
                .stream()
                .map(brand -> BrandListItemDTO.builder()
                        .id(brand.getId())
                        .parentId(brand.getParentId())
                        .code(brand.getCode())
                        .name(brand.getName())
                        .status(brand.getStatus())
                        .externalId(brand.getExternalId())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandListItemDTO getBrandById(Long id) {
        Brand brand = brandRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand not found with id: " + id));
        return BrandListItemDTO.builder()
                .id(brand.getId())
                .parentId(brand.getParentId())
                .code(brand.getCode())
                .name(brand.getName())
                .status(brand.getStatus())
                .externalId(brand.getExternalId())
                .build();
    }

    @Transactional
    public BrandListItemDTO createBrand(BrandRequest request) {
        validateRequest(request);

        Brand brand = Brand.builder()
                .code(request.getCode())
                .name(request.getName())
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .parentId(request.getParentId())
                .createdAt(java.time.LocalDateTime.now())
                .build();

        brand = brandRepo.save(brand);

        auditService.record(AuditAction.CATALOG_MUTATION, "BRAND", brand.getId().toString(),
                null, toJson(brand), "Brand created");

        return BrandListItemDTO.builder()
                .id(brand.getId())
                .parentId(brand.getParentId())
                .code(brand.getCode())
                .name(brand.getName())
                .status(brand.getStatus())
                .externalId(brand.getExternalId())
                .build();
    }

    @Transactional
    public BrandListItemDTO updateBrand(Long id, BrandRequest request) {
        validateRequest(request);

        Brand brand = brandRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand not found with id: " + id));

        String beforeJson = toJson(brand);

        brand.setCode(request.getCode());
        brand.setName(request.getName());
        brand.setStatus(request.getStatus() != null ? request.getStatus() : brand.getStatus());
        brand.setParentId(request.getParentId());

        brand = brandRepo.save(brand);

        auditService.record(AuditAction.CATALOG_MUTATION, "BRAND", id.toString(),
                beforeJson, toJson(brand), "Brand updated");

        return BrandListItemDTO.builder()
                .id(brand.getId())
                .parentId(brand.getParentId())
                .code(brand.getCode())
                .name(brand.getName())
                .status(brand.getStatus())
                .externalId(brand.getExternalId())
                .build();
    }

    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand not found with id: " + id));

        // Guard: reject if brand has children
        if (brandRepo.existsByParentId(id)) {
            throw new BadRequestException("Cannot delete brand with child brands. Deactivate instead.");
        }

        // Guard: reject if products reference this brand
        if (productRepo.existsByBrandId(id)) {
            throw new BadRequestException("Cannot delete brand referenced by products. Deactivate instead.");
        }

        String beforeJson = toJson(brand);
        brandRepo.delete(brand);

        auditService.record(AuditAction.CATALOG_MUTATION, "BRAND", id.toString(),
                beforeJson, null, "Brand deleted");
    }

    @Transactional
    public BrandListItemDTO setBrandStatus(Long id, Integer status) {
        Brand brand = brandRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand not found with id: " + id));

        String beforeJson = toJson(brand);
        brand.setStatus(status);
        brand = brandRepo.save(brand);

        auditService.record(AuditAction.CATALOG_MUTATION, "BRAND", id.toString(),
                beforeJson, toJson(brand), "Brand status changed to " + status);

        return BrandListItemDTO.builder()
                .id(brand.getId())
                .parentId(brand.getParentId())
                .code(brand.getCode())
                .name(brand.getName())
                .status(brand.getStatus())
                .externalId(brand.getExternalId())
                .build();
    }

    private void validateRequest(BrandRequest request) {
        if (request == null) {
            throw new BadRequestException("Brand payload is required");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Brand code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Brand name is required");
        }
        if (request.getParentId() != null && request.getParentId().equals(0L)) {
            request.setParentId(null);
        }
    }

    private String toJson(Brand brand) {
        if (brand == null) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(brand);
        } catch (Exception e) {
            return "{}";
        }
    }
}