package com.vn.sodu.nhanh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "nhanh_integration")
@Getter
@Setter
public class NhanhIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // businessId từ Nhanh
    @Column(nullable = false, unique = true)
    private Long businessId;

    // appId (để support multi app nếu cần)
    @Column(nullable = false)
    private String appId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    // expiredAt từ Nhanh là UNIX timestamp
    private Long expiredAt;

    private Long lastProductSyncTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}