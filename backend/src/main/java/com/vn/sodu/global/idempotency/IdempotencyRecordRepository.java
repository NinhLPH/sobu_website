package com.vn.sodu.global.idempotency;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select record
            from IdempotencyRecord record
            where record.scope = :scope
              and record.idempotencyKey = :idempotencyKey
            """)
    Optional<IdempotencyRecord> findForUpdate(
            @Param("scope") IdempotencyScope scope,
            @Param("idempotencyKey") String idempotencyKey
    );
}
