package com.vn.sodu.voucher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long>, JpaSpecificationExecutor<Voucher> {

    Optional<Voucher> findByCodeIgnoreCaseAndDeletedFalse(String code);

    default Optional<Voucher> findByCodeIgnoreCase(String code) {
        return findByCodeIgnoreCaseAndDeletedFalse(code);
    }

    List<Voucher> findByActiveTrueAndDeletedFalse();

    default List<Voucher> findByActiveTrue() {
        return findByActiveTrueAndDeletedFalse();
    }

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

    default boolean existsByCodeIgnoreCase(String code) {
        return existsByCodeIgnoreCaseAndDeletedFalse(code);
    }

    Page<Voucher> findByDeletedFalse(Pageable pageable);

    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 WHERE v.id = :id AND (v.usageLimit IS NULL OR v.usedCount < v.usageLimit) AND v.active = true AND v.deleted = false")
    int incrementUsedCountAtomic(@Param("id") Long id);
}
