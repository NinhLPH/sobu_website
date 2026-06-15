package com.vn.sodu.ui;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WebConfigRepo
        extends JpaRepository<WebsiteConfiguration, Long>, JpaSpecificationExecutor<WebsiteConfiguration> {
}