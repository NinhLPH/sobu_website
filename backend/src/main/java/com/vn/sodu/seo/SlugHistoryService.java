package com.vn.sodu.seo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlugHistoryService {

    private final SlugHistoryRepo slugHistoryRepo;

    @Transactional
    public void recordSlugChange(String entityType, Long entityId, String oldSlug, String newSlug) {
        if (oldSlug == null || oldSlug.isBlank() || newSlug == null || newSlug.isBlank()) {
            return;
        }
        if (oldSlug.equalsIgnoreCase(newSlug)) {
            return;
        }

        SlugHistory history = SlugHistory.builder()
                .entityType(entityType.toUpperCase())
                .entityId(entityId)
                .oldSlug(oldSlug.trim().toLowerCase())
                .currentSlug(newSlug.trim().toLowerCase())
                .httpStatus(301)
                .createdAt(LocalDateTime.now())
                .build();

        slugHistoryRepo.save(history);
        log.info("Recorded slug change for {} ID {}: {} -> {}", entityType, entityId, oldSlug, newSlug);
    }

    @Transactional(readOnly = true)
    public Optional<String> findCurrentSlug(String entityType, String oldSlug) {
        if (oldSlug == null || oldSlug.isBlank()) {
            return Optional.empty();
        }
        return slugHistoryRepo
                .findFirstByEntityTypeAndOldSlugOrderByCreatedAtDesc(entityType.toUpperCase(), oldSlug.trim().toLowerCase())
                .map(SlugHistory::getCurrentSlug);
    }
}
