package com.vn.sodu.nhanh.webhook;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NhanhWebhookEventLogRepository extends JpaRepository<NhanhWebhookEventLog, Long> {

    List<NhanhWebhookEventLog> findByStatusInAndAttemptCountLessThanOrderByReceivedAtAsc(
            List<NhanhWebhookEventLogStatus> statuses,
            int maxAttempts,
            Pageable pageable
    );
}
