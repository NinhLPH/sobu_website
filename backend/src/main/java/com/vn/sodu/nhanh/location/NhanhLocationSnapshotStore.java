package com.vn.sodu.nhanh.location;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.NhanhProperties;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class NhanhLocationSnapshotStore {

    private final NhanhLocationSnapshotRepository repository;
    private final NhanhProperties nhanhProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public NhanhLocationSnapshotStore(
            NhanhLocationSnapshotRepository repository,
            NhanhProperties nhanhProperties,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this(
                repository,
                nhanhProperties,
                objectMapper,
                Clock.systemUTC(),
                new TransactionTemplate(transactionManager));
    }

    NhanhLocationSnapshotStore(
            NhanhLocationSnapshotRepository repository,
            NhanhProperties nhanhProperties,
            ObjectMapper objectMapper,
            Clock clock,
            TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.nhanhProperties = nhanhProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    public Optional<LocationTreeResponse> load() {
        return repository.findById(snapshotId()).map(this::deserialize);
    }

    public boolean isExpiredOrMissing() {
        return repository.findById(snapshotId())
                .map(snapshot -> !clock.instant().isBefore(snapshot.getExpiresAt()))
                .orElse(true);
    }

    public void save(
            LocationTreeResponse tree,
            int cityCount,
            int districtCount,
            int wardCount) {
        String serialized;
        try {
            serialized = objectMapper.writeValueAsString(tree);
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Nhanh location snapshot could not be serialized", ex);
        }

        transactionTemplate.executeWithoutResult(status -> {
            NhanhLocationSnapshot snapshot = new NhanhLocationSnapshot();
            snapshot.setId(snapshotId());
            snapshot.setData(serialized);
            snapshot.setSyncedAt(tree.getCachedAt());
            snapshot.setExpiresAt(tree.getExpiresAt());
            snapshot.setCityCount(cityCount);
            snapshot.setDistrictCount(districtCount);
            snapshot.setWardCount(wardCount);
            repository.save(snapshot);
        });
    }

    private LocationTreeResponse deserialize(NhanhLocationSnapshot snapshot) {
        try {
            LocationTreeResponse response = objectMapper.readValue(
                    snapshot.getData(),
                    LocationTreeResponse.class);
            boolean stale = !clock.instant().isBefore(snapshot.getExpiresAt());
            return response.toBuilder()
                    .cachedAt(snapshot.getSyncedAt())
                    .expiresAt(snapshot.getExpiresAt())
                    .stale(stale)
                    .build();
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Stored Nhanh location snapshot could not be parsed", ex);
        }
    }

    private NhanhLocationSnapshotId snapshotId() {
        return new NhanhLocationSnapshotId(
                nhanhProperties.getBusinessId(),
                nhanhProperties.getLocation().getVersion());
    }
}
