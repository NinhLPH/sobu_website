package com.vn.sodu.nhanh.service;

import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import com.vn.sodu.nhanh.location.LocationDataUnavailableException;
import com.vn.sodu.nhanh.location.NhanhLocationSnapshotStore;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NhanhLocationServiceTest {

    @Test
    void readsOnlyFromPersistedSnapshot() {
        NhanhLocationSnapshotStore store = mock(NhanhLocationSnapshotStore.class);
        LocationTreeResponse expected = LocationTreeResponse.builder().build();
        when(store.load()).thenReturn(Optional.of(expected));

        LocationTreeResponse result = new NhanhLocationService(store).getLocations();

        assertSame(expected, result);
        verify(store).load();
    }

    @Test
    void missingSnapshotIsUnavailable() {
        NhanhLocationSnapshotStore store = mock(NhanhLocationSnapshotStore.class);
        when(store.load()).thenReturn(Optional.empty());

        assertThrows(
                LocationDataUnavailableException.class,
                () -> new NhanhLocationService(store).getLocations());
    }
}
