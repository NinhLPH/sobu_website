package com.vn.sodu.nhanh.service;

import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.location.LocationDataUnavailableException;
import com.vn.sodu.nhanh.location.NhanhLocationSnapshotStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NhanhLocationService {

    private final NhanhLocationSnapshotStore snapshotStore;

    public LocationTreeResponse getLocations() {
        return snapshotStore.load()
                .orElseThrow(LocationDataUnavailableException::new);
    }
}
