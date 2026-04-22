package com.vn.sodu.product.brand.service;

import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import com.vn.sodu.product.brand.dto.NhanhBrandListData;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import com.vn.sodu.product.dto.NhanhResponse;
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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandSyncService {

    private final BrandRepo brandRepo;
    private final BrandMapper brandMapper;

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
        String url = nhanhBrandsUrl;

        while (url != null && !url.isBlank()) {
            try {
                ResponseEntity<NhanhResponse<NhanhBrandListData>> response =
                        restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                null,
                                new ParameterizedTypeReference<NhanhResponse<NhanhBrandListData>>() {}
                        );

                NhanhResponse<NhanhBrandListData> body = response.getBody();
                if (body == null || body.getData() == null || body.getData().getBrands() == null) {
                    break;
                }

                result.addAll(body.getData().getBrands());

                if (body.getPaginator() != null) {
                    url = body.getPaginator().getNext();
                } else {
                    url = null;
                }
            } catch (RestClientException ex) {
                log.error("Failed to fetch brands from Nhanh API. url={}", url, ex);
                throw new RuntimeException("Nhanh API fetch failed", ex);
            }
        }

        return result;
    }
}
