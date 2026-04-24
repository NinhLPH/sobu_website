package com.vn.sodu.ui;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BannerRepo extends JpaRepository<Banner, Long>, JpaSpecificationExecutor<Banner> {
}
