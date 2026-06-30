package com.vn.sodu.ui.service;

import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.storage.StorageService;
import com.vn.sodu.ui.Banner;
import com.vn.sodu.ui.BannerRepo;
import com.vn.sodu.ui.dto.BannerDTO;
import com.vn.sodu.ui.dto.UpdateBannerRequest;
import com.vn.sodu.ui.mapper.BannerMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BannerServiceImplTest {

    @Test
    void updateBannerUpdatesEditableFieldsButKeepsSeededPosition() {
        BannerRepo bannerRepo = mock(BannerRepo.class);
        StorageService storageService = mock(StorageService.class);
        BannerServiceImpl service = new BannerServiceImpl(bannerRepo, new BannerMapper(), storageService);
        Banner banner = Banner.builder()
                .id(1L)
                .title("Hero")
                .imageUrl("/old.jpg")
                .linkUrl("/products")
                .displayOrder(1)
                .position("home_hero_carousel")
                .isActive(true)
                .startDate(LocalDateTime.parse("2026-05-01T00:00:00"))
                .endDate(LocalDateTime.parse("2026-06-01T00:00:00"))
                .deviceType(Banner.DeviceType.ALL)
                .build();
        when(bannerRepo.findById(1L)).thenReturn(Optional.of(banner));
        when(bannerRepo.save(any(Banner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BannerDTO dto = service.updateBanner(1L, UpdateBannerRequest.builder()
                .title("Hero updated")
                .imageUrl("/new.jpg")
                .linkUrl("/services")
                .displayOrder(3)
                .position("site_left_sidebar_banner")
                .isActive(false)
                .startDate(null)
                .endDate(null)
                .deviceType(Banner.DeviceType.WEB)
                .build(), null);

        assertThat(dto.getTitle()).isEqualTo("Hero updated");
        assertThat(dto.getPosition()).isEqualTo("home_hero_carousel");
        assertThat(dto.getIsActive()).isFalse();
        assertThat(dto.getStartDate()).isNull();
        assertThat(dto.getEndDate()).isNull();
        assertThat(dto.getDeviceType()).isEqualTo(Banner.DeviceType.WEB);
        verify(bannerRepo).save(banner);
    }

    @Test
    void updateBannerRejectsMissingImageUrl() {
        BannerRepo bannerRepo = mock(BannerRepo.class);
        BannerServiceImpl service = new BannerServiceImpl(bannerRepo, new BannerMapper(), mock(StorageService.class));
        when(bannerRepo.findById(1L)).thenReturn(Optional.of(Banner.builder()
                .id(1L)
                .title("Hero")
                .imageUrl("/old.jpg")
                .position("home_hero_carousel")
                .build()));

        assertThatThrownBy(() -> service.updateBanner(1L, UpdateBannerRequest.builder()
                .title("Hero")
                .imageUrl(" ")
                .build(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("image URL");
    }
}
