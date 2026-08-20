package com.vn.sodu.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Trace record for each import of the vendored administrative dataset.
 * Lets admins and support verify which release/version is live and when it was
 * imported, and provides a checksum for reconciliation.
 */
@Entity
@Table(name = "administrative_dataset_releases")
@Getter
@Setter
@NoArgsConstructor
public class AdministrativeDatasetRelease {

    @Id
    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "source", nullable = false, length = 500)
    private String source;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;
}