package com.vn.sodu.product.brand.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import com.vn.sodu.product.brand.dto.NhanhBrandListData;
import com.vn.sodu.product.brand.dto.NhanhBrandListResponse;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import com.vn.sodu.product.dto.NhanhResponse;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandSyncService {

    private final BrandRepo brandRepo;
    private final BrandMapper brandMapper;
    private final NhanhService nhanhService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nhanh.api.brands-url:}")
    private String nhanhBrandsUrl;

    public void syncBrands() {
        List<NhanhBrandDTO> brands = fetchAllBrands();

        if (brands.isEmpty()) {
            log.info("Nhanh sync skipped: no brands");
            return;
        }

        int success = 0;
        int failed = 0;

        for (NhanhBrandDTO dto : brands) {
            try {
                syncOne(dto);
                success++;
            } catch (Exception ex) {
                failed++;
                log.error("Sync failed for brandId={}", dto != null ? dto.getId() : null, ex);
            }
        }

        log.info("Nhanh sync done. total={}, success={}, failed={}",
                brands.size(), success, failed);
    }

    @Transactional
    public void syncOne(NhanhBrandDTO dto) {
        if (dto == null || dto.getId() == null) {
            return;
        }

        Brand brand = brandMapper.toEntity(dto);
        brandRepo.save(brand);
    }

    public List<NhanhBrandDTO> fetchAllBrands() {
        if (nhanhBrandsUrl == null || nhanhBrandsUrl.isBlank()) {
            throw new IllegalStateException("nhanh.api.brands-url is not configured");
        }

        List<NhanhBrandDTO> result = new ArrayList<>();
        int page = 1;
        String accessToken = nhanhService.getValidAccessToken();

        while (true) {
            try {
                Map<String, Object> body = Map.of(
                        "page", page,
                        "pageSize", 50
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + accessToken);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                ResponseEntity<NhanhBrandListResponse> response =
                        restTemplate.postForEntity(nhanhBrandsUrl, request, NhanhBrandListResponse.class);

                NhanhBrandListResponse respBody = response.getBody();

                if (respBody == null || respBody.getCode() != 1) {
                    log.error("Nhanh API returned code != 1: {}", respBody != null ? respBody.getCode() : "null");
                    break;
                }

                if (respBody.getData() == null || respBody.getData().getBrands() == null) {
                    break;
                }

                result.addAll(respBody.getData().getBrands());

                int totalPages = respBody.getData().getTotalPages();
                if (page >= totalPages) {
                    break;
                }

                page++;

            } catch (RestClientException ex) {
                log.error("Failed to fetch brands from Nhanh API. url={}, page={}", nhanhBrandsUrl, page, ex);
                throw new RuntimeException("Nhanh API fetch failed", ex);
            }
        }

        return result;
    }
}
