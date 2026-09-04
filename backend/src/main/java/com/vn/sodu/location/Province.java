package com.vn.sodu.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Province/city from the vendored post-July-2025 VietnamProvinces dataset.
 *
 * <p>The id is the stable administrative code from the source dataset. Records
 * that are replaced by a newer release are soft-disabled ({@code active=false})
 * and never hard-deleted so historical orders stay readable.</p>
 */
@Entity
@Table(name = "provinces")
@Getter
@Setter
@NoArgsConstructor
public class Province {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "dataset_version", nullable = false, length = 20)
    private String datasetVersion;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}