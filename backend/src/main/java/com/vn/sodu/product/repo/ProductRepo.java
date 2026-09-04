package com.vn.sodu.product.repo;

import com.vn.sodu.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByExternalId(Long externalId);
    Optional<Product> findFirstBySlug(String slug);
    default Optional<Product> findBySlug(String slug) {
        return findFirstBySlug(slug);
    }
    boolean existsBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    boolean existsByCategoryId(Long categoryId);
    boolean existsByBrandId(Long brandId);
    boolean existsByBadgeId(Long badgeId);

    @Modifying
    @Query("update Product p set p.badgeName = :badgeName, p.badgeColor = :badgeColor, p.badgeTextColor = :badgeTextColor, p.updatedAt = :updatedAt where p.badgeId = :badgeId")
    int updateBadgeSnapshot(@Param("badgeId") Long badgeId,
                            @Param("badgeName") String badgeName,
                            @Param("badgeColor") String badgeColor,
                            @Param("badgeTextColor") String badgeTextColor,
                            @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
