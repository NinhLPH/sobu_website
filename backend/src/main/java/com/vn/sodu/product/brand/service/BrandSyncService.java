package com.vn.sodu.product.brand.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.vn.sodu.nhanh.service.NhanhClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandSyncService {

    private final BrandRepo brandRepo;
    private final BrandMapper brandMapper;
    private final NhanhService nhanhService;

    private final NhanhClient nhanhClient;

    private static final String BRAND_LIST_PATH = "/v3.0/product/brand";

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
                if (syncOne(dto)) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception ex) {
                failed++;
                log.error("Sync failed for brandId={}", dto != null ? dto.getId() : null, ex);
            }
        }

        log.info("Nhanh sync done. total={}, success={}, failed={}",
                brands.size(), success, failed);
    }

    @Transactional
    public boolean syncOne(NhanhBrandDTO dto) {
        if (dto == null || dto.getId() == null) {
            log.warn("Skipping brand sync item with null payload or id");
            return false;
        }

        Brand brand = brandMapper.toEntity(dto);
        if (brand == null) {
            log.warn("Skipping brand sync item because mapper returned null for id={}", dto.getId());
            return false;
        }
        brandRepo.save(brand);
        return true;
    }

    public List<NhanhBrandDTO> fetchAllBrands() {
        String accessToken = nhanhService.getValidAccessToken();

        List<NhanhBrandDTO> result = new java.util.ArrayList<>();
        List<NhanhBrandDTO> rawList = nhanhClient.fetchAllPages(
                BRAND_LIST_PATH,
                accessToken,
                null,
                new org.springframework.core.ParameterizedTypeReference<NhanhResponse<List<NhanhBrandDTO>>>() {}
        );
        
        for (NhanhBrandDTO dto : rawList) {
            if (dto == null || dto.getId() == null) {
                log.warn("Skipping brand payload item with null dto or id");
                continue;
            }
            result.add(dto);
        }
        
        return result;
    }
}
