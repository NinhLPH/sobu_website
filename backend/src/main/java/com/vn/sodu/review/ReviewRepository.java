package com.vn.sodu.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByAccountIdAndProductId(Long accountId, Long productId);

    long countByProductIdAndStatus(Long productId, ReviewStatus status);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.product.id = :productId and r.status = :status")
    Double averageRatingByProductIdAndStatus(
            @Param("productId") Long productId,
            @Param("status") ReviewStatus status
    );

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
