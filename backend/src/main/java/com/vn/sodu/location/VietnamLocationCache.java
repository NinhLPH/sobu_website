package com.vn.sodu.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Single-row cache of the Vietnam administrative-division tree fetched from the
 * public Vietnam Map API. Used only while the Nhanh integration is disabled.
 */
@Entity
@Table(name = "vietnam_location_cache")
@Getter
@Setter
@NoArgsConstructor
public class VietnamLocationCache {

    @Id
    @Column(name = "id", nullable = false)
    private Long id = 1L;

    @Column(name = "data", nullable = false, columnDefinition = "LONGTEXT")
    private String data;

    @Column(name = "cached_at", nullable = false)
    private Instant cachedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "city_count", nullable = false)
    private int cityCount;

    @Column(name = "district_count", nullable = false)
    private int districtCount;

    @Column(name = "ward_count", nullable = false)
    private int wardCount;
}
