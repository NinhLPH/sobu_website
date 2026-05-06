package com.vn.sodu.request;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "request_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Column(length = 50)
    private String nhanhProductId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(length = 255)
    private String customName;

    private BigDecimal price;

    private Integer quantity;

    @Column(columnDefinition = "JSON")
    private String specs;

    @Column(length = 500)
    private String imageUrl;
}