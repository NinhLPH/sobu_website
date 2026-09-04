package com.vn.sodu.seo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SlugHistoryRepo extends JpaRepository<SlugHistory, Long> {
    Optional<SlugHistory> findFirstByEntityTypeAndOldSlugOrderByCreatedAtDesc(String entityType, String oldSlug);
}
