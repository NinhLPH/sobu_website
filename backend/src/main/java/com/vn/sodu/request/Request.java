package com.vn.sodu.request;

import com.vn.sodu.product.brand.Brand;
import com.vn.sodu.product.category.Category;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name ="requests")
public class Request {
    @Id @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String requestCode; // SOBU-REQ-2024123456

    @Column(nullable = false)
    private String customerPhone; // KEY MATCHING

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    private OrderType type;

    private BigDecimal totalAmount;

    @Column(columnDefinition = "JSON")
    private String customRequirements;

    @Column(columnDefinition = "JSON")
    private String uploadedImages;

    // Nhanh sync
    private String nhanhOrderId;
    private String nhanhOrderCode;

    // Relations
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL)
    private List<RequestItem> items;

    @ManyToOne
    private User admin;
}
