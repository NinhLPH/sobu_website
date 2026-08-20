package com.vn.sodu.product.brand;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "brands",
        indexes = {
                @Index(name = "idx_brand_parent", columnList = "parentId")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true)
    private Long externalId;

    private String code;

    private String name;

    private Integer status;

    private Long parentId;

    private LocalDateTime createdAt;

    // ===== RELATION (READ ONLY) =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentId", insertable = false, updatable = false)
    private Brand parent;
}
