package com.vn.sodu.product.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.dto.NhanhProductListData;
import com.vn.sodu.product.dto.NhanhProductListResponse;
import com.vn.sodu.product.dto.NhanhResponse;
import com.vn.sodu.product.mapper.ProductMapper;
import com.vn.sodu.product.repo.ProductAttributeRepo;
import com.vn.sodu.product.repo.ProductImageRepo;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.repo.ProductUnitRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSyncService {

    private final ProductRepo productRepo;
    private final ProductUnitRepo productUnitRepo;
    private final ProductAttributeRepo productAttributeRepo;
    private final ProductImageRepo productImageRepo;
    private final ProductMapper productMapper;
    private final NhanhService nhanhService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nhanh.api.products-url:}")
    private String nhanhProductsUrl;

    public void syncProducts() {
        List<NhanhProductDTO> products = fetchAllProducts();

        if (products.isEmpty()) {
            log.info("Nhanh sync skipped: no products");
            return;
        }

        int success = 0;
        int failed = 0;

        for (NhanhProductDTO dto : products) {
            try {
                syncOne(dto);
                success++;
            } catch (Exception ex) {
                failed++;
                log.error("Sync failed for productId={}", dto.getId(), ex);
            }
        }

        log.info("Nhanh sync done. total={}, success={}, failed={}",
                products.size(), success, failed);
    }

    // =============================
    // SYNC 1 PRODUCT (transaction riêng)
    // =============================
    @Transactional
    public void syncOne(NhanhProductDTO dto) {

        if (dto == null || dto.getId() == null) {
            return;
        }

        Product product = productMapper.toEntity(dto);
        productRepo.save(product);

        Long productId = product.getId();

        // delete old children
        productUnitRepo.deleteByProductId(productId);
        productAttributeRepo.deleteByProductId(productId);
        productImageRepo.deleteByProductId(productId);

        // insert new
        List<ProductUnit> units = productMapper.toUnits(productId, dto);
        List<ProductAttribute> attributes = productMapper.toAttributes(productId, dto);
        List<ProductImage> images = productMapper.toImages(productId, dto);

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

    // =============================
    // FETCH ALL (pagination)
    // =============================
    public List<NhanhProductDTO> fetchAllProducts() {

        if (nhanhProductsUrl == null || nhanhProductsUrl.isBlank()) {
            throw new IllegalStateException("nhanh.api.products-url is not configured");
        }

        String accessToken = nhanhService.getValidAccessToken();

        return fetchAllProductsWithFetcher(page -> {
            Map<String, Object> body = Map.of(
                    "page", page,
                    "pageSize", 50
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<NhanhProductListResponse> response =
                    restTemplate.postForEntity(nhanhProductsUrl, request, NhanhProductListResponse.class);

            return response.getBody();
        });
    }

    // Package-private for testing: supply a fetcher function that returns NhanhProductListResponse for a page
    List<NhanhProductDTO> fetchAllProductsWithFetcher(java.util.function.Function<Integer, NhanhProductListResponse> fetcher) {
        List<NhanhProductDTO> result = new java.util.ArrayList<>();

        int page = 1;
        int totalPages = 1;

        do {
            NhanhProductListResponse resp;
            try {
                resp = fetcher.apply(page);
            } catch (Exception ex) {
                log.error("Failed to fetch products for page={}", page, ex);
                throw new RuntimeException("Nhanh API fetch failed", ex);
            }

            if (resp == null) {
                log.error("Nhanh response is null for page={}", page);
                throw new RuntimeException("Invalid response: null");
            }

            if (resp.getData() == null) {
                log.error("Nhanh response data is null for page={}", page);
                throw new RuntimeException("Invalid response: data is null");
            }

            if (resp.getCode() != 1) {
                log.error("Nhanh API returned non-success code for page={}: {}", page, resp.getCode());
                throw new RuntimeException("Nhanh API error, code=" + resp.getCode());
            }

            if (resp.getData().getProducts() != null) {
                result.addAll(resp.getData().getProducts());
            } else {
                log.warn("Products is null for page={}", page);
            }

            totalPages = resp.getData().getTotalPages();
            page++;
        } while (page <= totalPages);

        return result;
    }
}
