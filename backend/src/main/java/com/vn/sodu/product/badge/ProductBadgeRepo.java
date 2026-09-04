package com.vn.sodu.product.badge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductBadgeRepo extends JpaRepository<ProductBadge, Long> {
    Optional<ProductBadge> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
