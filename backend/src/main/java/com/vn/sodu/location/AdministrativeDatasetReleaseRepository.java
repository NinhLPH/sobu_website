package com.vn.sodu.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministrativeDatasetReleaseRepository
        extends JpaRepository<AdministrativeDatasetRelease, String> {

    Optional<AdministrativeDatasetRelease> findTopByOrderByImportedAtDesc();
}