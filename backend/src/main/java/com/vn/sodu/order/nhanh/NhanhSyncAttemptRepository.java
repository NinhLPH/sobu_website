package com.vn.sodu.order.nhanh;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NhanhSyncAttemptRepository extends JpaRepository<NhanhSyncAttempt, Long> {

    Optional<NhanhSyncAttempt> findByIdempotencyKey(String idempotencyKey);

    Optional<NhanhSyncAttempt> findTopByBaseKeyOrderByCreatedAtDesc(String baseKey);
}
