package com.vn.sodu.ui;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface WebConfigRepo
        extends JpaRepository<WebsiteConfiguration, Long>, JpaSpecificationExecutor<WebsiteConfiguration> {
    List<WebsiteConfiguration> findByKeyIn(Collection<String> keys);
}
