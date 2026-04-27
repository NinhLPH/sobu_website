package com.vn.sodu.product;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "product_units",
        indexes = {
                @Index(name = "idx_unit_product", columnList = "productId")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUnit {

    @Id
    private Long id;

    @Column(name = "productId", nullable = false)
    private Long productId;

    private String name;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal wholesalePrice;

    // ===== RELATION (READ ONLY) =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", insertable = false, updatable = false)
    private Product product;
}