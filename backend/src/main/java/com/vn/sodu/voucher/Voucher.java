package com.vn.sodu.voucher;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vouchers", indexes = {
    @Index(name = "idx_voucher_code", columnList = "code"),
    @Index(name = "idx_voucher_active_slot", columnList = "active, slot, deleted")
})
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VoucherType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private VoucherSlot slot = VoucherSlot.ORDER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private VoucherScope scope = VoucherScope.ALL;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private GeoScope geoScope = GeoScope.ALL;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal value;

    @Column(precision = 19, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal minOrderValue;

    private Integer usageLimit;

    @Builder.Default
    @Column(nullable = false)
    private Integer usedCount = 0;

    @Builder.Default
    @Column(name = "auto_apply", nullable = false)
    private Boolean autoApply = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "voucher_product_ids", joinColumns = @JoinColumn(name = "voucher_id"))
    @Column(name = "product_id")
    @Builder.Default
    private Set<Long> applicableProductIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "voucher_category_ids", joinColumns = @JoinColumn(name = "voucher_id"))
    @Column(name = "category_id")
    @Builder.Default
    private Set<Long> applicableCategoryIds = new HashSet<>();

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
