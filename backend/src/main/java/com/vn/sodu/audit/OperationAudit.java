package com.vn.sodu.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable audit trail for sensitive local operations. Records who changed
 * what, the before/after values, and why, so administrators can explain every
 * sensitive change after the fact.
 */
@Entity
@Table(name = "operation_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    /** Actor identity (username or "system" / "deployment"). */
    @Column(nullable = false, length = 120)
    private String actor;

    /** Actor type, e.g. account, system, deployment. */
    @Column(nullable = false, length = 40)
    private String actorType;

    /** Type of the audited target, e.g. ORDER, PRODUCT, INTEGRATION_FLAG. */
    @Column(nullable = false, length = 60)
    private String targetType;

    /** Identifier of the audited target (order id, product id, ...). */
    @Column(length = 120)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String beforeValue;

    @Column(columnDefinition = "TEXT")
    private String afterValue;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
