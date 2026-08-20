package com.vn.sodu.location;

import com.vn.sodu.location.dto.AddressDatasetDTO;
import com.vn.sodu.location.dto.ProvinceListResponseDTO;
import com.vn.sodu.location.dto.ProvinceResponseDTO;
import com.vn.sodu.location.dto.WardListResponseDTO;
import com.vn.sodu.location.dto.WardResponseDTO;
import com.vn.sodu.nhanh.location.LocationDataUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reads the vendored, versioned Vietnam administrative dataset (province -> ward).
 *
 * <p>This is the Sobu-owned source of truth for storefront address selection in
 * local mode. It serves active records only for new selections; inactive records
 * remain in the database so historical order snapshots stay readable.</p>
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;
    private final AdministrativeDatasetReleaseRepository releaseRepository;

    @Transactional(readOnly = true)
    public ProvinceListResponseDTO listProvinces() {
        String version = currentDatasetVersion();
        List<Province> provinces = provinceRepository.findByActiveTrueOrderByNameAsc();
        if (provinces.isEmpty()) {
            throw new LocationDataUnavailableException();
        }
        List<ProvinceResponseDTO> items = provinces.stream()
                .map(province -> new ProvinceResponseDTO(province.getId(), province.getName()))
                .toList();
        return ProvinceListResponseDTO.builder()
                .datasetVersion(version)
                .provinces(items)
                .build();
    }

    @Transactional(readOnly = true)
    public WardListResponseDTO listWards(Long provinceId) {
        if (provinceId == null) {
            throw new IllegalArgumentException("Province id is required");
        }
        String version = currentDatasetVersion();
        List<Ward> wards = wardRepository.findByProvinceIdAndActiveTrueOrderByNameAsc(provinceId);
        List<WardResponseDTO> items = wards.stream()
                .map(ward -> new WardResponseDTO(ward.getId(), ward.getName()))
                .toList();
        return WardListResponseDTO.builder()
                .datasetVersion(version)
                .wards(items)
                .build();
    }

    @Transactional(readOnly = true)
    public AddressDatasetDTO currentDataset() {
        String version = currentDatasetVersion();
        long provinceCount = provinceRepository.count();
        long wardCount = wardRepository.count();
        return releaseRepository.findTopByOrderByImportedAtDesc()
                .map(release -> AddressDatasetDTO.builder()
                        .version(release.getVersion())
                        .source(release.getSource())
                        .importedAt(release.getImportedAt())
                        .checksum(release.getChecksum())
                        .provinceCount(provinceCount)
                        .wardCount(wardCount)
                        .build())
                .orElseGet(() -> AddressDatasetDTO.builder()
                        .version(version)
                        .provinceCount(provinceCount)
                        .wardCount(wardCount)
                        .build());
    }

    /**
     * Backend validation that a submitted ward id exists, is active, and belongs
     * to the submitted province id. Returns true only when every check passes.
     */
    @Transactional(readOnly = true)
    public boolean isWardInProvince(Long wardId, Long provinceId) {
        if (wardId == null || provinceId == null) {
            return false;
        }
        return wardRepository.existsByIdAndProvinceId(wardId, provinceId);
    }

    private String currentDatasetVersion() {
        return releaseRepository.findTopByOrderByImportedAtDesc()
                .map(AdministrativeDatasetRelease::getVersion)
                .orElse("unknown");
    }
}