package com.vn.sodu.location;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.location.dto.AddressDatasetDTO;
import com.vn.sodu.location.dto.ProvinceListResponseDTO;
import com.vn.sodu.location.dto.WardListResponseDTO;
import com.vn.sodu.nhanh.location.LocationDataUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for the Sobu-owned, versioned Vietnam administrative dataset
 * (province -> ward). The storefront reads these APIs; it never calls Nhanh or
 * an external map service for location data.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/address")
@Tag(name = "Address dataset", description = "Public endpoints for the local Vietnam administrative dataset (province -> ward)")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/provinces")
    @Operation(summary = "List active provinces", description = "Returns the active provinces of the current dataset release.")
    public ResponseEntity<ApiResponseDTO<ProvinceListResponseDTO>> getProvinces() {
        try {
            ProvinceListResponseDTO response = addressService.listProvinces();
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Provinces retrieved"));
        } catch (LocationDataUnavailableException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponseDTO.<ProvinceListResponseDTO>error(
                            "Address dataset is not available", "LOCATION_DATA_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE.value()));
        }
    }

    @GetMapping("/wards")
    @Operation(summary = "List active wards of a province", description = "Returns the active wards belonging to the given province id.")
    public ResponseEntity<ApiResponseDTO<WardListResponseDTO>> getWards(
            @RequestParam(name = "provinceId") Long provinceId) {
        WardListResponseDTO response = addressService.listWards(provinceId);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Wards retrieved"));
    }

    @GetMapping("/datasets/current")
    @Operation(summary = "Current dataset release", description = "Returns the live dataset version, source, import time and row counts for admin/support reconciliation.")
    public ResponseEntity<ApiResponseDTO<AddressDatasetDTO>> getCurrentDataset() {
        AddressDatasetDTO response = addressService.currentDataset();
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Current address dataset retrieved"));
    }
}