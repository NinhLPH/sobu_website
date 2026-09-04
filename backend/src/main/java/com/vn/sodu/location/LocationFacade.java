package com.vn.sodu.location;

import com.vn.sodu.integration.NhanhEnabled;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.service.NhanhLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Chooses the location source based on the Nhanh master switch:
 * Nhanh snapshot when enabled, Vietnam Map API (local mode) when disabled.
 */
@Service
@RequiredArgsConstructor
public class LocationFacade {

    private final NhanhEnabled nhanhEnabled;
    private final NhanhLocationService nhanhLocationService;
    private final VietnamMapLocationProvider vietnamMapLocationProvider;

    public LocationTreeResponse getLocations() {
        if (nhanhEnabled.isEnabled()) {
            return nhanhLocationService.getLocations();
        }
        return vietnamMapLocationProvider.getLocations();
    }
}
