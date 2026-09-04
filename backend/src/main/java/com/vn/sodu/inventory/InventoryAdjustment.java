package com.vn.sodu.inventory;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable inventory adjustment ledger row. Records every stock movement —
 * who, what, why, and the resulting balance — so administrators can reconcile
 * a product balance from its adjustment history.
 */
@Entity
@Table(name = "inventory_adjustments",
        indexes = {
            @Index(name = "idx_inventory_adjustment_product", columnList = "productId"),
            @Index(name = "idx_inventory_adjustment_order", columnList = "orderId")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryAdjustmentType type;

    /** Signed quantity moved by this entry. */
    @Column(nullable = false)
    private Double quantityDelta;

    /** Resulting balance after this entry (stockAvailable for order types). */
    @Column(nullable = false)
    private Double balanceAfter;

    /** Order reference when the movement is order-driven (reservation/release). */
    private Long orderId;

    private String orderCode;

    /** Free-text reason for manual adjustments. */
    @Column(length = 1000)
    private String note;

    @Column(nullable = false, length = 120)
    private String actor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}