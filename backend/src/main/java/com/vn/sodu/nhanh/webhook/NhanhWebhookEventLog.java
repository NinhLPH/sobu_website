package com.vn.sodu.nhanh.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "nhanh_webhook_event_logs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_name", "business_id", "payload_hash"})
}, indexes = {
        @Index(name = "idx_nhanh_webhook_status", columnList = "status"),
        @Index(name = "idx_nhanh_webhook_received_at", columnList = "received_at")
})
public class NhanhWebhookEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_name", nullable = false, length = 50)
    private String eventName;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "business_id", length = 50)
    private String businessId;

    @Column(name = "external_object_id", length = 100)
    private String externalObjectId;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "authorization_present", nullable = false)
    private boolean authorizationPresent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NhanhWebhookEventLogStatus status = NhanhWebhookEventLogStatus.RECEIVED;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
