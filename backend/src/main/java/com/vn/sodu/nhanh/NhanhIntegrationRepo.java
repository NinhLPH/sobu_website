package com.vn.sodu.nhanh;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NhanhIntegrationRepo extends JpaRepository<NhanhIntegration, Long> {
    Optional<NhanhIntegration> findFirstByOrderByCreatedAtDesc();
}
