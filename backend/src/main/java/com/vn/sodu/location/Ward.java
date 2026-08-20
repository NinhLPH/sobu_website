package com.vn.sodu.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ward/commune from the vendored post-July-2025 VietnamProvinces dataset.
 *
 * <p>The id is the stable administrative code from the source dataset. Each
 * ward belongs to exactly one province; the id and {@code province_id} are
 * indexed for lookup and for backend validation that a submitted ward belongs
 * to the submitted province.</p>
 */
@Entity
@Table(
        name = "wards",
        indexes = {
                @Index(name = "idx_wards_province_name", columnList = "province_id, name")
        })
@Getter
@Setter
@NoArgsConstructor
public class Ward {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "province_id", nullable = false)
    private Long provinceId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "dataset_version", nullable = false, length = 20)
    private String datasetVersion;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}