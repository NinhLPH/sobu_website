package com.vn.sodu.product.service;


import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.dto.ProductDetailDTO;
import com.vn.sodu.product.dto.ProductFilterRequest;
import com.vn.sodu.product.dto.ProductListItemDTO;
import com.vn.sodu.product.mapper.ProductMapper;
import com.vn.sodu.product.repo.ProductAttributeRepo;
import com.vn.sodu.product.repo.ProductImageRepo;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.repo.ProductUnitRepo;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {
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
            Map.entry("updatedat", "updatedAt")
    );

    private final ProductRepo productRepo;
    private final ProductImageRepo productImageRepo;
    private final ProductAttributeRepo productAttributeRepo;
    private final ProductUnitRepo productUnitRepo;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductListItemDTO> getAllProducts() {
        return productRepo.findAll()
                .stream()
                .map(productMapper::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductListItemDTO> getPublicProducts(ProductFilterRequest request) {
        ProductFilterRequest safeRequest = request == null ? new ProductFilterRequest() : request;
        Pageable pageable = toPageable(safeRequest);
        Specification<Product> specification = buildSpecification(safeRequest);

        return productRepo.findAll(specification, pageable)
                .map(productMapper::toListItem);
    }

    @Transactional(readOnly = true)
    public Page<ProductListItemDTO> searchProducts(String searchTerm, ProductFilterRequest request) {
        ProductFilterRequest safeRequest = request == null ? new ProductFilterRequest() : request;
        safeRequest.setSearch(searchTerm);
        return getPublicProducts(safeRequest);
    }

    @Transactional(readOnly = true)
    public ProductDetailDTO getProductDetailById(long id) {
        Product product = productRepo.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        List<ProductImage> imageList = productImageRepo.findByProductId(id);
        List<ProductUnit> productUnitList = productUnitRepo.findByProductId(id);
        List<ProductAttribute> productAttributeList = productAttributeRepo.findByProductId(id);

        return productMapper.toDetail(product, productUnitList, productAttributeList, imageList);
    }

    private Pageable toPageable(ProductFilterRequest request) {
        String sortBy = resolveSortBy(request.getSortBy());
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
        return PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "id";
        }

        return SORT_FIELD_MAPPING.getOrDefault(sortBy.trim().toLowerCase(Locale.ROOT), "id");
    }

    private Specification<Product> buildSpecification(ProductFilterRequest request) {
        return (root, query, cb) -> {
            query.distinct(true);
            Predicate predicate = cb.conjunction();

            if (request.getCategoryId() != null) {
                predicate = cb.and(predicate, cb.equal(root.<Long>get("categoryId"), request.getCategoryId()));
            }
            if (request.getBrandId() != null) {
                predicate = cb.and(predicate, cb.equal(root.<Long>get("brandId"), request.getBrandId()));
            }
            if (request.getMinPrice() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.<BigDecimal>get("retailPrice"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.<BigDecimal>get("retailPrice"), request.getMaxPrice()));
            }
            if (request.getInStock() != null) {
                if (request.getInStock()) {
                    predicate = cb.and(predicate, cb.greaterThan(root.<Double>get("stockAvailable"), 0d));
                } else {
                    predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.<Double>get("stockAvailable"), 0d));
                }
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

            return predicate;
        };
    }
}
