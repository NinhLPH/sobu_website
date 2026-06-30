package com.vn.sodu.ui.controller;

import com.vn.sodu.global.dto.ApiResponseDTO;
import com.vn.sodu.ui.Banner;
import com.vn.sodu.ui.dto.BannerDTO;
import com.vn.sodu.ui.service.BannerService;
import com.vn.sodu.ui.service.WebConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicUiControllerTest {

    @Mock
    private BannerService bannerService;

    @Mock
    private WebConfigService webConfigService;

    @InjectMocks
    private PublicUiController publicUiController;

    @Test
    void getActiveBannersPassesRawPositionString() {
        BannerDTO dto = BannerDTO.builder()
                .id(1L)
                .position("checkout_top")
                .deviceType(Banner.DeviceType.WEB)
                .build();
        when(bannerService.getActiveBanners(Banner.DeviceType.WEB, "checkout_top"))
                .thenReturn(List.of(dto));

        ResponseEntity<ApiResponseDTO<List<BannerDTO>>> response =
                publicUiController.getActiveBanners(Banner.DeviceType.WEB, "checkout_top");

        verify(bannerService).getActiveBanners(Banner.DeviceType.WEB, "checkout_top");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData())
                .extracting(BannerDTO::getPosition)
                .containsExactly("checkout_top");
    }

    @Test
    void getActiveBannersPassesNullPositionWhenMissing() {
        when(bannerService.getActiveBanners(Banner.DeviceType.ALL, null))
                .thenReturn(List.of());

        ResponseEntity<ApiResponseDTO<List<BannerDTO>>> response =
                publicUiController.getActiveBanners(Banner.DeviceType.ALL, null);

        verify(bannerService).getActiveBanners(Banner.DeviceType.ALL, null);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEmpty();
    }
}
