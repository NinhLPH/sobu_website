package com.vn.sodu.nhanh.location;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class NhanhLocationSnapshotId implements Serializable {

    @Column(name = "business_id", nullable = false, length = 64)
    private String businessId;

    @Column(name = "location_version", nullable = false, length = 32)
    private String locationVersion;
}
