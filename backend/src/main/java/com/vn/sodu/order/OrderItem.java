package com.vn.sodu.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(length = 50)
    private String nhanhProductId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(length = 1000)
    private String note;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    @Column(precision = 19, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false)
    private Integer quantity;
}
