package com.vn.sodu.product.service;

import com.vn.sodu.product.Product;
import com.vn.sodu.product.ProductAttribute;
import com.vn.sodu.product.ProductImage;
import com.vn.sodu.product.ProductUnit;
import com.vn.sodu.product.dto.NhanhProductDTO;
import com.vn.sodu.product.dto.NhanhProductListData;
import com.vn.sodu.product.dto.NhanhResponse;
import com.vn.sodu.product.mapper.ProductMapper;
import com.vn.sodu.product.repo.ProductAttributeRepo;
import com.vn.sodu.product.repo.ProductImageRepo;
import com.vn.sodu.product.repo.ProductRepo;
import com.vn.sodu.product.repo.ProductUnitRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSyncService {

    private final ProductRepo productRepo;
    private final ProductUnitRepo productUnitRepo;
    private final ProductAttributeRepo productAttributeRepo;
    private final ProductImageRepo productImageRepo;
    private final ProductMapper productMapper;

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

        List<NhanhProductDTO> result = new java.util.ArrayList<>();

        String url = nhanhProductsUrl;

        while (url != null && !url.isBlank()) {

            try {
                ResponseEntity<NhanhResponse<NhanhProductListData>> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                null,
                                new ParameterizedTypeReference<NhanhResponse<NhanhProductListData>>() {}
                        );

                NhanhResponse<NhanhProductListData> body = response.getBody();

                if (body == null || body.getData() == null || body.getData().getProducts() == null) {
                    break;
                }

                result.addAll(body.getData().getProducts());

                // pagination next
                // If Nhanh still uses Paginator.next, we keep it. 
                // But the task says we have page and totalPages.
                // Usually Nhanh v3 uses Paginator for next URL.
                if (body.getPaginator() != null) {
                    url = body.getPaginator().getNext();
                } else {
                    // Fallback to page increment if needed, but let's stick to Paginator if it exists
                    url = null;
                }

            } catch (RestClientException ex) {
                log.error("Failed to fetch products from Nhanh API. url={}", url, ex);
                throw new RuntimeException("Nhanh API fetch failed", ex);
            }
        }

        return result;
    }
}
