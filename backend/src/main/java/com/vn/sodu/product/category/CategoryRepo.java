package com.vn.sodu.product.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepo extends JpaRepository<Category, Long> {
    Optional<Category> findByExternalId(Long externalId);
    Optional<Category> findFirstBySlug(String slug);
    default Optional<Category> findBySlug(String slug) {
        return findFirstBySlug(slug);
    }
    boolean existsBySlug(String slug);
    boolean existsByParentId(Long parentId);
}
