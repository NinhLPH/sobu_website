package com.vn.sodu.ui;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StaticPageRepo extends JpaRepository<StaticPage, Long>, JpaSpecificationExecutor<StaticPage> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    Optional<StaticPage> findBySlugAndIsPublishedTrue(String slug);
}
