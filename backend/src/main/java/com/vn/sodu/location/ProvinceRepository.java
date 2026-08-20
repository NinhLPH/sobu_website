package com.vn.sodu.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProvinceRepository extends JpaRepository<Province, Long> {

    List<Province> findByActiveTrueOrderByNameAsc();

    Optional<Province> findByIdAndActiveTrue(Long id);
}