package com.vn.sodu.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private String url;

    @Column(name = "alt_text")
    private String altText;

    private String caption;

    private Integer width;

    private Integer height;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_avatar")
    @Builder.Default
    private Boolean isAvatar = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", insertable = false, updatable = false)
    private Product product;
}
