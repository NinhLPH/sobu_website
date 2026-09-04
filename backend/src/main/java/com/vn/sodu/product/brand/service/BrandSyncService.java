package com.vn.sodu.product.brand.service;

import com.vn.sodu.integration.NhanhEnabled;
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
import org.springframework.scheduling.annotation.Scheduled;
import com.vn.sodu.nhanh.service.NhanhClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(name = "integration.nhanh.enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
@Slf4j
public class BrandSyncService {

    private final BrandRepo brandRepo;
    private final BrandMapper brandMapper;
    private final NhanhService nhanhService;

    private final NhanhClient nhanhClient;
    private final NhanhEnabled nhanhEnabled;

    private static final String BRAND_LIST_PATH = "/v3.0/product/brand";

    @Scheduled(cron = "${nhanh.sync.cron:0 0 */12 * * *}")
    public synchronized void syncBrands() {
        if (!nhanhEnabled.isEnabled()) {
            log.debug("Nhanh integration disabled — skipping brand sync");
            return;
        }
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

        int parentResolved = 0;
        int parentFailed = 0;
        for (NhanhBrandDTO dto : brands) {
            try {
                if (resolveParent(dto)) {
                    parentResolved++;
                } else {
                    parentFailed++;
                }
            } catch (Exception ex) {
                parentFailed++;
                log.error("Parent resolve failed for brandId={}", dto != null ? dto.getId() : null, ex);
            }
        }

        log.info("Nhanh sync done. total={}, success={}, failed={}",
                brands.size(), success, failed);
        log.info("Nhanh brand parent resolve done. resolved={}, failed={}",
                parentResolved, parentFailed);
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

        // Set external_id for upsert mapping
        brand.setExternalId(dto.getId());

        // If existing, keep PK; otherwise leave id null for IDENTITY generation
        java.util.Optional<Brand> existing = brandRepo.findByExternalId(dto.getId());
        if (existing.isPresent()) {
            brand.setId(existing.get().getId());
        }

        brand.setParentId(null);
        brand.setParent(null);
        brandRepo.save(brand);
        return true;
    }

    @Transactional
    boolean resolveParent(NhanhBrandDTO dto) {
        if (dto == null || dto.getId() == null) {
            return false;
        }

        Long parentId = normalizeParentId(dto.getParentId());
        if (parentId == null) {
            return true;
        }

        java.util.Optional<Brand> childByExternal = brandRepo.findByExternalId(dto.getId());
        if (childByExternal.isEmpty()) {
            log.warn("Skipping parent assignment because brand externalId={} is missing", dto.getId());
            return false;
        }

        java.util.Optional<Brand> parentByExternal = brandRepo.findByExternalId(parentId);
        if (parentByExternal.isEmpty()) {
            log.warn("Skipping parent assignment for brand externalId={} because parent externalId={} is missing",
                    dto.getId(), parentId);
            return false;
        }

        Brand brand = childByExternal.get();
        Long parentPk = parentByExternal.get().getId();
        brand.setParentId(parentPk);
        brand.setParent(null);
        brandRepo.save(brand);
        return true;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId != null && parentId > 0 ? parentId : null;
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
