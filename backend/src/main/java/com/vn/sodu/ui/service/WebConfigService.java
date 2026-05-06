package com.vn.sodu.ui.service;

import com.vn.sodu.global.dto.PageResponse;
import com.vn.sodu.global.dto.SearchRequest;
import com.vn.sodu.ui.dto.WebsiteConfigurationDTO;
import com.vn.sodu.ui.dto.WebsiteConfigurationRequest;

import java.util.List;

public interface WebConfigService {
    WebsiteConfigurationDTO createConfig(WebsiteConfigurationRequest request);
    WebsiteConfigurationDTO updateConfig(Long id, WebsiteConfigurationRequest request);
    WebsiteConfigurationDTO getConfigById(Long id);
    void deleteConfig(Long id);
    PageResponse<WebsiteConfigurationDTO> getAllConfigs(SearchRequest request);
    List<WebsiteConfigurationDTO> getPublicConfigs();
    WebsiteConfigurationDTO getConfigByKey(String key);
}
