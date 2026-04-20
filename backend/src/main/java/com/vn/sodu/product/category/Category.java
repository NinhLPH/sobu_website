package com.vn.sodu.product.category;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_category_parent", columnList = "parentId")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    private Long id;

    @Column(name = "parentId")
    private Long parentId;

    private String code;

    private String name;

    private Integer order;

    private String image;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer status;

    // ===== RELATION (READ ONLY) =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentId", insertable = false, updatable = false)
    private Category parent;
}