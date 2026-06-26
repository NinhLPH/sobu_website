package com.vn.sodu.ui;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebsiteConfigurationSeederTest {

    @Test
    void runInsertsOnlyMissingDefaultConfigs() {
        WebConfigRepo webConfigRepo = mock(WebConfigRepo.class);
        WebsiteConfiguration existingConfig = WebsiteConfiguration.builder()
                .key("primary_color")
                .value("#custom")
                .type(WebsiteConfiguration.ConfigType.color)
                .groupName("THEME")
                .build();
        when(webConfigRepo.findByKeyIn(anyCollection())).thenReturn(List.of(existingConfig));
        WebsiteConfigurationSeeder seeder = new WebsiteConfigurationSeeder(webConfigRepo);

        seeder.run(null);

        ArgumentCaptor<List<WebsiteConfiguration>> captor = ArgumentCaptor.forClass(List.class);
        verify(webConfigRepo).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(WebsiteConfiguration::getKey)
                .doesNotContain("primary_color")
                .contains("seo_default_title", "business_low_stock_threshold");
    }

    @Test
    void runDoesNotSaveWhenAllDefaultConfigsExist() {
        WebConfigRepo webConfigRepo = mock(WebConfigRepo.class);
        when(webConfigRepo.findByKeyIn(anyCollection())).thenAnswer(invocation -> ((List<String>) invocation.getArgument(0)).stream()
                .map(key -> WebsiteConfiguration.builder()
                        .key(key)
                        .value("existing")
                        .type(WebsiteConfiguration.ConfigType.text)
                        .groupName("GENERAL")
                        .build())
                .toList());
        WebsiteConfigurationSeeder seeder = new WebsiteConfigurationSeeder(webConfigRepo);

        seeder.run(null);

        verify(webConfigRepo, never()).saveAll(anyCollection());
    }
}
