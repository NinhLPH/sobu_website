package com.vn.sodu.product.service;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.audit.AuditAction;
import com.vn.sodu.audit.AuditService;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.badge.ProductBadgeRepo;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.category.CategoryRepo;
import com.vn.sodu.product.dto.ProductDetailDTO;
import com.vn.sodu.product.dto.ProductFilterRequest;
import com.vn.sodu.product.dto.ProductListItemDTO;
import com.vn.sodu.product.dto.ProductCreateRequest;
import com.vn.sodu.product.dto.ProductUpdateRequest;
import com.vn.sodu.product.mapper.AdminProductMapper;
import com.vn.sodu.product.mapper.ProductMapper;
import com.vn.sodu.product.repo.ProductAttributeRepo;
import com.vn.sodu.product.repo.ProductImageRepo;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.repo.ProductUnitRepo;
import com.vn.sodu.review.ReviewRepository;
import com.vn.sodu.review.ReviewStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private static final Map<String, String> SORT_FIELD_MAPPING = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("name", "name"),
            Map.entry("code", "code"),
            Map.entry("price", "retailPrice"),
            Map.entry("retailprice", "retailPrice"),
            Map.entry("status", "status"),
            Map.entry("brandname", "brandName"),
            Map.entry("categoryname", "categoryName"),
            Map.entry("stockavailable", "stockAvailable"),
            Map.entry("createdat", "createdAt"),
            Map.entry("updatedat", "updatedAt"),
            Map.entry("active", "active")
    );

    private final ProductRepo productRepo;
    private final ProductImageRepo productImageRepo;
    private final ProductAttributeRepo productAttributeRepo;
    private final ProductUnitRepo productUnitRepo;
    private final ProductMapper productMapper;
    private final AdminProductMapper adminProductMapper;
    private final ReviewRepository reviewRepository;
    private final AuditService auditService;
    private final BrandRepo brandRepo;
    private final CategoryRepo categoryRepo;
    private final ProductBadgeRepo productBadgeRepo;

    @Transactional(readOnly = true)
    public PageResponse<ProductListItemDTO> getAllProducts(ProductFilterRequest request) {
        ProductFilterRequest safeRequest = request == null ? new ProductFilterRequest() : request;
        Pageable pageable = toPageable(safeRequest);
        Specification<Product> specification = buildAdminSpecification(safeRequest);

        Page<ProductListItemDTO> page = productRepo.findAll(specification, pageable)
                .map(productMapper::toListItem)
                .map(this::withReviewSummary);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetailById(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
        List<ProductImage> imageList = productImageRepo.findByProductId(id);
        List<ProductUnit> productUnitList = productUnitRepo.findByProductId(id);
        List<ProductAttribute> productAttributeList = productAttributeRepo.findByProductId(id);

        return withReviewSummary(productMapper.toDetail(product, productUnitList, productAttributeList, imageList));
    }

    @Transactional
    public ProductDetailDTO createProduct(ProductCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Product payload is required");
        }
        validateSaleConfiguration(request.getRetailPrice(), request.getOldPrice(),
                request.getSaleValidFrom(), request.getSaleValidThrough());
        Product product = adminProductMapper.toEntity(request);
        resolveReferences(product, request.getCategoryId(), request.getBrandId(), request.getBadgeId());
        product = productRepo.save(product);

        // Save child entities
        Long productId = product.getId();
        saveChildren(productId, request);

        auditService.record(AuditAction.CATALOG_MUTATION, "PRODUCT", productId.toString(),
                null, toJson(product), "Product created");

        return getProductDetailById(productId);
    }

    @Transactional
    public ProductDetailDTO updateProduct(Long id, ProductUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Product payload is required");
        }
        Product existing = productRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
        BigDecimal effectiveConfiguredPrice = request.getRetailPrice() != null
                ? request.getRetailPrice()
                : existing.getRetailPrice();
        if (effectiveConfiguredPrice != null) {
            validateSaleConfiguration(effectiveConfiguredPrice, request.getOldPrice(),
                    request.getSaleValidFrom(), request.getSaleValidThrough());
        }

        String beforeJson = toJson(existing);
        adminProductMapper.updateEntity(existing, request);
        if (request.getBadgeId() == null) {
            existing.setBadgeId(null);
            existing.setBadgeName(null);
            existing.setBadgeColor(null);
            existing.setBadgeTextColor(null);
        }
        resolveReferences(existing, request.getCategoryId(), request.getBrandId(), request.getBadgeId());
        productRepo.save(existing);

        // Replace child entities (delete old + save new)
        replaceChildren(id, request);

        auditService.record(AuditAction.CATALOG_MUTATION, "PRODUCT", id.toString(),
                beforeJson, toJson(existing), "Product updated");

        return getProductDetailById(id);
    }

    @Transactional
    public ProductDetailDTO setActive(Long id, Boolean active, String reason) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        String beforeJson = toJson(product);
        product.setActive(active);
        product.setUpdatedAt(java.time.LocalDateTime.now());
        productRepo.save(product);

        auditService.record(AuditAction.CATALOG_MUTATION, "PRODUCT", id.toString(),
                beforeJson, toJson(product),
                (active ? "Product activated" : "Product deactivated") + (reason != null ? ": " + reason : ""));

        return getProductDetailById(id);
    }

    @Transactional
    public ProductDetailDTO archiveProduct(Long id, String reason) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        String beforeJson = toJson(product);
        product.setStatus("ARCHIVED");
        product.setActive(false);
        product.setUpdatedAt(java.time.LocalDateTime.now());
        productRepo.save(product);

        auditService.record(AuditAction.CATALOG_MUTATION, "PRODUCT", id.toString(),
                beforeJson, toJson(product),
                "Product archived" + (reason != null ? ": " + reason : ""));

        return getProductDetailById(id);
    }

    private void saveChildren(Long productId, ProductCreateRequest request) {
        List<ProductUnit> units = adminProductMapper.toUnits(productId, request);
        List<ProductAttribute> attributes = adminProductMapper.toAttributes(productId, request);
        List<ProductImage> images = adminProductMapper.toImages(productId, request);

        if (units != null && !units.isEmpty()) {
            productUnitRepo.saveAll(units);
        }
        if (attributes != null && !attributes.isEmpty()) {
            productAttributeRepo.saveAll(attributes);
        }
        if (images != null && !images.isEmpty()) {
            productImageRepo.saveAll(images);
        }
    }

    private void replaceChildren(Long productId, ProductUpdateRequest request) {
        productUnitRepo.deleteByProductId(productId);
        productAttributeRepo.deleteByProductId(productId);
        productImageRepo.deleteByProductId(productId);

        List<ProductUnit> units = adminProductMapper.toUnits(productId, request);
        List<ProductAttribute> attributes = adminProductMapper.toAttributes(productId, request);
        List<ProductImage> images = adminProductMapper.toImages(productId, request);

        if (units != null && !units.isEmpty()) {
            productUnitRepo.saveAll(units);
        }
        if (attributes != null && !attributes.isEmpty()) {
            productAttributeRepo.saveAll(attributes);
        }
        if (images != null && !images.isEmpty()) {
            productImageRepo.saveAll(images);
        }
    }

    private void resolveReferences(Product product, Long categoryId, Long brandId, Long badgeId) {
        if (brandId != null) {
            brandRepo.findById(brandId).ifPresent(brand -> product.setBrandName(brand.getName()));
        }
        if (categoryId != null) {
            categoryRepo.findById(categoryId).ifPresent(category -> product.setCategoryName(category.getName()));
        }
        if (badgeId != null) {
            productBadgeRepo.findById(badgeId).ifPresentOrElse(
                    badge -> {
                        if ("SALE".equalsIgnoreCase(badge.getName())) {
                            throw new BadRequestException("SALE is a system tag and cannot be assigned manually");
                        }
                        if (badge.getStatus() == null || badge.getStatus() != 1) {
                            throw new BadRequestException("Inactive product badges cannot be assigned");
                        }
                        product.setBadgeId(badge.getId());
                        product.setBadgeName(badge.getName());
                        product.setBadgeColor(badge.getColor());
                        product.setBadgeTextColor(badge.getTextColor());
                    },
                    () -> {
                        throw new BadRequestException("Product badge not found with id: " + badgeId);
                    }
            );
        }
    }

    private ProductListItemDTO withReviewSummary(ProductListItemDTO dto) {
        if (dto == null || dto.getId() == null) {
            return dto;
        }
        dto.setReviewsCount(reviewRepository.countByProductIdAndStatus(dto.getId(), ReviewStatus.PUBLISHED));
        dto.setAverageRating(reviewRepository.averageRatingByProductIdAndStatus(dto.getId(), ReviewStatus.PUBLISHED));
        return dto;
    }

    private ProductDetailDTO withReviewSummary(ProductDetailDTO dto) {
        if (dto == null || dto.getId() == null) {
            return dto;
        }
        dto.setReviewsCount(reviewRepository.countByProductIdAndStatus(dto.getId(), ReviewStatus.PUBLISHED));
        dto.setAverageRating(reviewRepository.averageRatingByProductIdAndStatus(dto.getId(), ReviewStatus.PUBLISHED));
        return dto;
    }

    private Pageable toPageable(ProductFilterRequest request) {
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(
                    request.getSortDirection() == null ? "DESC" : request.getSortDirection()
            );
        } catch (IllegalArgumentException ex) {
            direction = Sort.Direction.DESC;
        }

        int page = Math.max(request.getPage(), 0);
        int pageSize = request.getPageSize() > 0 ? Math.min(request.getPageSize(), 100) : 20;
        if (ProductSaleCriteria.isComputedSort(request.getSortBy())) {
            return PageRequest.of(page, pageSize);
        }
        String sortBy = resolveSortBy(request.getSortBy());
        return PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "id";
        }
        return SORT_FIELD_MAPPING.getOrDefault(sortBy.trim().toLowerCase(Locale.ROOT), "id");
    }

    private Specification<Product> buildAdminSpecification(ProductFilterRequest request) {
        return (root, query, cb) -> {
            query.distinct(true);
            Predicate predicate = cb.conjunction();
            LocalDateTime now = LocalDateTime.now();

            if (request.getCategoryId() != null) {
                predicate = cb.and(predicate, cb.equal(root.<Long>get("categoryId"), request.getCategoryId()));
            }
            if (request.getBrandId() != null) {
                predicate = cb.and(predicate, cb.equal(root.<Long>get("brandId"), request.getBrandId()));
            }
            if (request.getMinPrice() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(
                        ProductSaleCriteria.effectivePrice(root, cb, now), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(
                        ProductSaleCriteria.effectivePrice(root, cb, now), request.getMaxPrice()));
            }
            if (request.getInStock() != null) {
                if (request.getInStock()) {
                    predicate = cb.and(predicate, cb.greaterThan(root.<Double>get("stockAvailable"), 0d));
                } else {
                    predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.<Double>get("stockAvailable"), 0d));
                }
            }
            if (request.getOnSale() != null) {
                Predicate activeSale = ProductSaleCriteria.activeSale(root, cb, now);
                predicate = cb.and(predicate, request.getOnSale() ? activeSale : cb.not(activeSale));
            }
            if (request.getActive() != null) {
                predicate = cb.and(predicate, cb.equal(root.<Boolean>get("active"), request.getActive()));
            }
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.<String>get("status"), request.getStatus()));
            }

            String search = request.getSearch();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.<String>get("name")), pattern),
                        cb.like(cb.lower(root.<String>get("code")), pattern),
                        cb.like(cb.lower(root.<String>get("barcode")), pattern),
                        cb.like(cb.lower(root.<String>get("brandName")), pattern),
                        cb.like(cb.lower(root.<String>get("categoryName")), pattern),
                        cb.like(cb.lower(root.<String>get("description")), pattern),
                        cb.like(cb.lower(root.<String>get("content")), pattern)
                );
                predicate = cb.and(predicate, searchPredicate);
            }

            Sort.Direction direction;
            try {
                direction = Sort.Direction.fromString(
                        request.getSortDirection() == null ? "DESC" : request.getSortDirection());
            } catch (IllegalArgumentException ex) {
                direction = Sort.Direction.DESC;
            }
            ProductSaleCriteria.applyComputedSort(root, query, cb, request.getSortBy(), direction, now);
            return predicate;
        };
    }

    private void validateSaleConfiguration(
            BigDecimal price,
            BigDecimal oldPrice,
            LocalDateTime saleValidFrom,
            LocalDateTime saleValidThrough
    ) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Product price must be greater than or equal to 0");
        }
        if (oldPrice != null && oldPrice.compareTo(price) <= 0) {
            throw new BadRequestException("oldPrice must be greater than price");
        }
        if ((saleValidFrom != null || saleValidThrough != null) && oldPrice == null) {
            throw new BadRequestException("A sale validity period requires oldPrice");
        }
        if (saleValidFrom != null && saleValidThrough != null && saleValidThrough.isBefore(saleValidFrom)) {
            throw new BadRequestException("saleValidThrough must not be before saleValidFrom");
        }
    }

    private String toJson(Product product) {
        if (product == null) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(product);
        } catch (Exception e) {
            return "{}";
        }
    }
}
