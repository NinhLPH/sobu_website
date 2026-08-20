package com.vn.sodu.product.brand;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepo extends JpaRepository<Brand, Long> {
    Optional<Brand> findByExternalId(Long externalId);
    Optional<Brand> findFirstBySlug(String slug);
    default Optional<Brand> findBySlug(String slug) {
        return findFirstBySlug(slug);
    }
    boolean existsBySlug(String slug);
    boolean existsByParentId(Long parentId);
}
