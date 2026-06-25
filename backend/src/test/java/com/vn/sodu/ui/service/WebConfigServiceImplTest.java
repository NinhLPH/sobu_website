package com.vn.sodu.ui.service;

import com.vn.sodu.ui.WebConfigRepo;
import com.vn.sodu.ui.WebsiteConfiguration;
import com.vn.sodu.ui.dto.BulkWebsiteConfigurationRequest;
import com.vn.sodu.ui.dto.WebsiteConfigurationDTO;
import com.vn.sodu.ui.mapper.WebsiteConfigurationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebConfigServiceImplTest {

    @Mock
    private WebConfigRepo webConfigRepo;

    private WebConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WebConfigServiceImpl(webConfigRepo, new WebsiteConfigurationMapper());
    }

    @Test
    void bulkUpdateConfigsUpdatesValuesByKey() {
        WebsiteConfiguration primaryColor = WebsiteConfiguration.builder()
                .id(1L)
                .key("primary_color")
                .value("#00618e")
                .type(WebsiteConfiguration.ConfigType.color)
                .groupName("THEME")
                .isPublic(true)
                .isActive(true)
                .build();
        WebsiteConfiguration seoTitle = WebsiteConfiguration.builder()
                .id(2L)
                .key("seo_default_title")
                .value("Old title")
                .type(WebsiteConfiguration.ConfigType.text)
                .groupName("SEO")
                .isPublic(true)
                .isActive(true)
                .build();
        when(webConfigRepo.findByKeyIn(List.of("primary_color", "seo_default_title")))
                .thenReturn(List.of(primaryColor, seoTitle));
        when(webConfigRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<WebsiteConfigurationDTO> configs = service.bulkUpdateConfigs(List.of(
                BulkWebsiteConfigurationRequest.builder()
                        .key("primary_color")
                        .value("#FF0000")
                        .build(),
                BulkWebsiteConfigurationRequest.builder()
                        .key("seo_default_title")
                        .value("Sobu")
                        .build()
        ));

        assertThat(configs)
                .extracting(WebsiteConfigurationDTO::getValue)
                .containsExactly("#FF0000", "Sobu");
        verify(webConfigRepo).saveAll(List.of(primaryColor, seoTitle));
    }

    @Test
    void bulkUpdateConfigsRejectsMissingKeysBeforeSaving() {
        when(webConfigRepo.findByKeyIn(List.of("primary_color", "missing_key")))
                .thenReturn(List.of(WebsiteConfiguration.builder()
                        .key("primary_color")
                        .value("#00618e")
                        .type(WebsiteConfiguration.ConfigType.color)
                        .build()));

        assertThatThrownBy(() -> service.bulkUpdateConfigs(List.of(
                BulkWebsiteConfigurationRequest.builder()
                        .key("primary_color")
                        .value("#FF0000")
                        .build(),
                BulkWebsiteConfigurationRequest.builder()
                        .key("missing_key")
                        .value("value")
                        .build()
        )))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("missing_key");
        verify(webConfigRepo, never()).saveAll(anyList());
    }

    @Test
    void bulkUpdateConfigsRejectsDuplicateKeysBeforeSaving() {
        assertThatThrownBy(() -> service.bulkUpdateConfigs(List.of(
                BulkWebsiteConfigurationRequest.builder()
                        .key("primary_color")
                        .value("#FF0000")
                        .build(),
                BulkWebsiteConfigurationRequest.builder()
                        .key("primary_color")
                        .value("#00FF00")
                        .build()
        )))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Duplicate configuration key");
        verify(webConfigRepo, never()).findByKeyIn(anyList());
        verify(webConfigRepo, never()).saveAll(anyList());
    }
}
