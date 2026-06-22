package com.vn.sodu.nhanh.location;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "nhanh_location_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class NhanhLocationSnapshot {

    @EmbeddedId
    private NhanhLocationSnapshotId id;

    @Column(name = "data", nullable = false, columnDefinition = "LONGTEXT")
    private String data;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "city_count", nullable = false)
    private int cityCount;

    @Column(name = "district_count", nullable = false)
    private int districtCount;

    @Column(name = "ward_count", nullable = false)
    private int wardCount;
}
