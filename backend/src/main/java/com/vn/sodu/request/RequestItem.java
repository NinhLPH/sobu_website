package com.vn.sodu.request;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "request_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "request")
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

    @Column(length = 1000)
    private String note;

    @Column(columnDefinition = "JSON")
    private String metadataJson;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;
}
