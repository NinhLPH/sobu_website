package com.vn.sodu.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_videos")
@Getter
@Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ProductVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private String title;

    private String src;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", insertable = false, updatable = false)
    private Product product;
}
