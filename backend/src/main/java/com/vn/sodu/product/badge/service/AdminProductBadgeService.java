package com.vn.sodu.product.badge.service;

import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.audit.AuditService;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.product.badge.ProductBadge;
import com.vn.sodu.product.badge.ProductBadgeRepo;
import com.vn.sodu.product.badge.dto.ProductBadgeDTO;
import com.vn.sodu.product.badge.dto.ProductBadgeRequest;
import com.vn.sodu.product.repo.ProductRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminProductBadgeService {

    private static final String HEX_COLOR_PATTERN = "^#[0-9A-Fa-f]{6}$";

    private final ProductBadgeRepo productBadgeRepo;
    private final ProductRepo productRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ProductBadgeDTO> getAllBadges() {
        return productBadgeRepo.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductBadgeDTO getBadgeById(Long id) {
        return toDto(findBadge(id));
    }

    @Transactional
    public ProductBadgeDTO createBadge(ProductBadgeRequest request) {
        validateRequest(request, null);

        ProductBadge badge = ProductBadge.builder()
                .name(request.getName().trim())
                .color(request.getColor().trim())
                .textColor(request.getTextColor().trim())
                .status(request.getStatus() != null ? request.getStatus() : 1)
                .createdAt(LocalDateTime.now())
                .build();

        badge = productBadgeRepo.save(badge);

        auditService.record(AuditAction.CATALOG_MUTATION, "PRODUCT_BADGE", badge.getId().toString(),
                null, toJson(badge), "Product badge created");

        return toDto(badge);
    }

    @Transactional
    public ProductBadgeDTO updateBadge(Long id, ProductBadgeRequest request) {
        validateRequest(request, id);

        ProductBadge badge = findBadge(id);
        String beforeJson = toJson(badge);

        String newName = request.getName().trim();
        String newColor = request.getColor().trim();
        String newTextColor = request.getTextColor().trim();

        boolean snapshotChanged = !newName.equals(badge.getName())
                || !newColor.equals(badge.getColor())
                || !newTextColor.equals(badge.getTextColor());

        badge.setName(newName);
        badge.setColor(newColor);
        badge.setTextColor(newTextColor);
        badge.setStatus(request.getStatus() != null ? request.getStatus() : badge.getStatus());
        productBadgeRepo.save(badge);

        if (snapshotChanged) {
            productRepo.updateBadgeSnapshot(id, newName, newColor, newTextColor, LocalDateTime.now());
        }

        auditService.record(AuditAction.CATALOG_MUTATION, "PRODUCT_BADGE", id.toString(),
                beforeJson, toJson(badge), "Product badge updated");

        return toDto(badge);
    }

    @Transactional
    public void deleteBadge(Long id) {
        ProductBadge badge = findBadge(id);

        if (productRepo.existsByBadgeId(id)) {
            throw new BadRequestException("Cannot delete badge referenced by products. Deactivate instead.");
        }

        String beforeJson = toJson(badge);
        productBadgeRepo.delete(badge);

        auditService.record(AuditAction.CATALOG_MUTATION, "PRODUCT_BADGE", id.toString(),
                beforeJson, null, "Product badge deleted");
    }

    @Transactional
    public ProductBadgeDTO setBadgeStatus(Long id, Integer status) {
        ProductBadge badge = findBadge(id);

        String beforeJson = toJson(badge);
        badge.setStatus(status);
        badge = productBadgeRepo.save(badge);

        auditService.record(AuditAction.CATALOG_MUTATION, "PRODUCT_BADGE", id.toString(),
                beforeJson, toJson(badge), "Product badge status changed to " + status);

        return toDto(badge);
    }

    private ProductBadge findBadge(Long id) {
        return productBadgeRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product badge not found with id: " + id));
    }

    private void validateRequest(ProductBadgeRequest request, Long currentId) {
        if (request == null) {
            throw new BadRequestException("Product badge payload is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Product badge name is required");
        }
        String name = request.getName().trim();
        productBadgeRepo.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Product badge with name '" + name + "' already exists");
                });
        validateColor(request.getColor(), "color");
        validateColor(request.getTextColor(), "textColor");
        if (request.getColor() != null && request.getColor().equalsIgnoreCase(request.getTextColor())) {
            throw new BadRequestException("Badge color and textColor must be different");
        }
    }

    private void validateColor(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Product badge " + field + " is required");
        }
        if (!value.trim().matches(HEX_COLOR_PATTERN)) {
            throw new BadRequestException("Product badge " + field + " must be a hex color like #RRGGBB");
        }
    }

    private ProductBadgeDTO toDto(ProductBadge badge) {
        if (badge == null) {
            return null;
        }
        return ProductBadgeDTO.builder()
                .id(badge.getId())
                .name(badge.getName())
                .color(badge.getColor())
                .textColor(badge.getTextColor())
                .status(badge.getStatus())
                .createdAt(badge.getCreatedAt())
                .build();
    }

    private String toJson(ProductBadge badge) {
        if (badge == null) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(badge);
        } catch (Exception e) {
            return "{}";
        }
    }
}
