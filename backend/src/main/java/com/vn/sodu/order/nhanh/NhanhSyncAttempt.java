package com.vn.sodu.order.nhanh;

import com.vn.sodu.order.Order;
import com.vn.sodu.payment.OrderPayment;
import jakarta.persistence.*;
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
@Table(name = "nhanh_sync_attempts")
public class NhanhSyncAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private OrderPayment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NhanhSyncOperationType operationType;

    @Column(nullable = false, length = 160)
    private String baseKey;

    @Column(nullable = false, unique = true, length = 200)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NhanhSyncAttemptStatus status = NhanhSyncAttemptStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column(columnDefinition = "TEXT")
    private String lastMessage;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Builder.Default
    private Integer retryCount = 0;

    private LocalDateTime nextRetryAt;

    private LocalDateTime lastAttemptAt;

    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
