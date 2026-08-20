package com.vn.sodu.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WardRepository extends JpaRepository<Ward, Long> {

    List<Ward> findByProvinceIdAndActiveTrueOrderByNameAsc(Long provinceId);

    Optional<Ward> findByIdAndActiveTrue(Long id);

    boolean existsByIdAndProvinceId(Long id, Long provinceId);
}