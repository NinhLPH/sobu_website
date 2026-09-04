package com.vn.sodu.location;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.sodu.global.exception.ExternalServiceException;
import com.vn.sodu.nhanh.dto.LocationTreeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Persists and reads the cached Vietnam Map location tree.
 */
@Service
public class VietnamLocationCacheStore {

    private static final Long CACHE_ID = 1L;

    private final VietnamLocationCacheRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public VietnamLocationCacheStore(
            VietnamLocationCacheRepository repository,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this(
                repository,
                objectMapper,
                Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")),
                new TransactionTemplate(transactionManager));
    }

    VietnamLocationCacheStore(
            VietnamLocationCacheRepository repository,
            ObjectMapper objectMapper,
            Clock clock,
            TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    public Optional<LocationTreeResponse> load() {
        return repository.findById(CACHE_ID).map(this::deserialize);
    }

    public void save(LocationTreeResponse tree, int cityCount, int districtCount, int wardCount) {
        String serialized;
        try {
            serialized = objectMapper.writeValueAsString(tree);
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Vietnam location cache could not be serialized", ex);
        }

        transactionTemplate.executeWithoutResult(status -> {
            VietnamLocationCache cache = new VietnamLocationCache();
            cache.setId(CACHE_ID);
            cache.setData(serialized);
            cache.setCachedAt(tree.getCachedAt());
            cache.setExpiresAt(tree.getExpiresAt());
            cache.setCityCount(cityCount);
            cache.setDistrictCount(districtCount);
            cache.setWardCount(wardCount);
            repository.save(cache);
        });
    }

    private LocationTreeResponse deserialize(VietnamLocationCache cache) {
        try {
            LocationTreeResponse response = objectMapper.readValue(
                    cache.getData(),
                    LocationTreeResponse.class);
            boolean stale = !clock.instant().isBefore(cache.getExpiresAt());
            return response.toBuilder()
                    .cachedAt(cache.getCachedAt())
                    .expiresAt(cache.getExpiresAt())
                    .stale(stale)
                    .build();
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Stored Vietnam location cache could not be parsed", ex);
        }
    }
}
