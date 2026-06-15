package com.vn.sodu.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_attributes",
        indexes = {
                @Index(name = "idx_attr_product", columnList = "productId")
        }
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ProductAttribute {

    @Id
    private Long id;

    @Column(name = "productId", nullable = false)
    private Long productId;

    private String name;

    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", insertable = false, updatable = false)
    private Product product;
}