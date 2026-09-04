package com.vn.sodu.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.integration.IntegrationProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Thin client for the public Vietnam administrative-division API
 * (https://provinces.open-api.vn). Used to collect Vietnamese locations for
 * the shipping address form while the Nhanh integration is disabled.
 */
@Service
public class VietnamMapApiClient {

    private final RestTemplate restTemplate;
    private final IntegrationProperties integrationProperties;

    public VietnamMapApiClient(RestTemplate restTemplate, IntegrationProperties integrationProperties) {
        this.restTemplate = restTemplate;
        this.integrationProperties = integrationProperties;
    }

    public List<VietnamProvince> fetchProvinces() {
        try {
            VietnamProvince[] provinces = restTemplate.getForObject(baseUrl() + "/", VietnamProvince[].class);
            return provinces == null ? List.of() : List.of(provinces);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Vietnam location API fetch of provinces failed", ex);
        }
    }

    public VietnamProvinceDetail fetchProvinceDetail(Long code) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl())
                .replacePath("/p/" + code)
                .queryParam("depth", 3)
                .toUriString();
        try {
            return restTemplate.getForObject(url, VietnamProvinceDetail.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Vietnam location API fetch of province " + code + " failed", ex);
        }
    }

    private String baseUrl() {
        String base = integrationProperties.getNhanh().getVietnamMap().getBaseUrl();
        if (base == null || base.isBlank()) {
            return "https://provinces.open-api.vn/api";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VietnamProvince {
        private Long code;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VietnamProvinceDetail {
        private Long code;
        private String name;
        private List<VietnamDistrict> districts;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VietnamDistrict {
        private Long code;
        private String name;
        private List<VietnamWard> wards;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VietnamWard {
        private Long code;
        private String name;
    }
}
