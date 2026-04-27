package com.vn.sodu.product.brand.service;

import com.vn.sodu.nhanh.service.NhanhService;
import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.brand.BrandRepo;
import com.vn.sodu.product.brand.dto.NhanhBrandDTO;
import com.vn.sodu.product.brand.mapper.BrandMapper;
import com.vn.sodu.product.dto.NhanhResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandSyncService {

    private final BrandRepo brandRepo;
    private final BrandMapper brandMapper;
    private final NhanhService nhanhService;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BRAND_LIST_PATH = "/v3.0/product/brand";

    @Value("${nhanh.base-url:}")
    private String nhanhBaseUrl;

    @Value("${nhanh.client-id:}")
    private String clientId;

    @Value("${nhanh.business-id:}")
    private String businessId;

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
        String url = buildBrandListUrl();
        String accessToken = nhanhService.getValidAccessToken();

        return fetchAllBrandsWithFetcher(nextCursor -> {
            Map<String, Object> body = buildBrandListRequestBody(nextCursor);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);

            log.info(url);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            Supplier<NhanhResponse<List<NhanhBrandDTO>>> call = () -> {
                ResponseEntity<String> raw = restTemplate.postForEntity(url, request, String.class);
                log.info("Dữ liệu thô từ Nhanh: {}", raw.getBody());
                ResponseEntity<NhanhResponse<List<NhanhBrandDTO>>> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        new ParameterizedTypeReference<NhanhResponse<List<NhanhBrandDTO>>>() {}
                );
                return response.getBody();
            };

            return withRetry(call, 3, 500);
        });
    }

    String buildBrandListUrl() {
        if (nhanhBaseUrl == null || nhanhBaseUrl.isBlank()) {
            throw new IllegalStateException("nhanh.base-url is not configured");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("nhanh.client-id is not configured");
        }
        if (businessId == null || businessId.isBlank()) {
            throw new IllegalStateException("nhanh.business-id is not configured");
        }

        return UriComponentsBuilder.fromHttpUrl(nhanhBaseUrl)
                .replacePath(BRAND_LIST_PATH)
                .queryParam("appId", clientId)
                .queryParam("businessId", businessId)
                .toUriString();
    }

    Map<String, Object> buildBrandListRequestBody(Object nextCursor) {
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> paginator = new HashMap<>();
        paginator.put("size", 50);
        if (nextCursor != null) {
            paginator.put("next", nextCursor);
        }
        body.put("paginator", paginator);
        return body;
    }

    List<NhanhBrandDTO> fetchAllBrandsWithFetcher(java.util.function.Function<Object, NhanhResponse<List<NhanhBrandDTO>>> fetcher) {
        List<NhanhBrandDTO> result = new ArrayList<>();
        Object nextCursor = null;
        int requestCount = 0;

        while (true) {
            requestCount++;
            NhanhResponse<List<NhanhBrandDTO>> resp;
            try {
                resp = fetcher.apply(nextCursor);
            } catch (Exception ex) {
                log.error("Failed to fetch brands from Nhanh at request={}", requestCount, ex);
                throw new RuntimeException("Nhanh API fetch failed", ex);
            }

            if (resp == null) {
                log.error("Nhanh response is null at request={}", requestCount);
                throw new RuntimeException("Invalid response: null");
            }

            if (resp.getCode() != 1) {
                log.error("Nhanh API returned non-success code at request={}: {}", requestCount, resp.getCode());
                throw new RuntimeException("Nhanh API error, code=" + resp.getCode());
            }

            if (resp.getData() == null) {
                log.error("Nhanh response data is null at request={}", requestCount);
                throw new RuntimeException("Invalid response: data is null");
            }

            for (NhanhBrandDTO dto : resp.getData()) {
                if (dto == null || dto.getId() == null) {
                    log.warn("Skipping brand payload item with null dto or id");
                    continue;
                }
                result.add(dto);
            }

            if (resp.getPaginator() == null || resp.getPaginator().getNext() == null) {
                break;
            }

            nextCursor = resp.getPaginator().getNext();
        }

        return result;
    }

    private <T> T withRetry(java.util.function.Supplier<T> supplier, int maxAttempts, long initialDelayMs) {
        int attempt = 0;
        long delay = initialDelayMs;

        while (true) {
            attempt++;
            try {
                return supplier.get();
            } catch (RuntimeException ex) {
                if (attempt >= maxAttempts) {
                    throw ex;
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                delay *= 2;
            }
        }
    }
}
