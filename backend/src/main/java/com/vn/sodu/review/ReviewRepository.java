package com.vn.sodu.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByAccountIdAndProductId(Long accountId, Long productId);

    @EntityGraph(attributePaths = {"account", "product", "order"})
    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"account", "product", "order"})
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"account", "product", "order"})
    Page<Review> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"account", "product", "order"})
    Optional<Review> findById(Long id);
}
