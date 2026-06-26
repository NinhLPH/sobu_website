package com.vn.sodu.ui.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.ui.WebsiteConfiguration;
import com.vn.sodu.ui.dto.BulkWebsiteConfigurationRequest;
import com.vn.sodu.ui.dto.WebsiteConfigurationDTO;
import com.vn.sodu.ui.service.WebConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminWebConfigControllerTest {

    @Test
    void bulkUpdateConfigsDelegatesToService() {
        WebConfigService webConfigService = mock(WebConfigService.class);
        AdminWebConfigController controller = new AdminWebConfigController(webConfigService);
        List<BulkWebsiteConfigurationRequest> request = List.of(
                BulkWebsiteConfigurationRequest.builder()
                        .key("primary_color")
                        .value("#FF0000")
                        .build()
        );
        List<WebsiteConfigurationDTO> serviceResponse = List.of(WebsiteConfigurationDTO.builder()
                .id(1L)
                .key("primary_color")
                .value("#FF0000")
                .type(WebsiteConfiguration.ConfigType.color)
                .groupName("THEME")
                .build());
        when(webConfigService.bulkUpdateConfigs(request)).thenReturn(serviceResponse);

        ResponseEntity<ApiResponseDTO<List<WebsiteConfigurationDTO>>> response =
                controller.bulkUpdateConfigs(request);

        verify(webConfigService).bulkUpdateConfigs(request);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(serviceResponse);
    }
}
