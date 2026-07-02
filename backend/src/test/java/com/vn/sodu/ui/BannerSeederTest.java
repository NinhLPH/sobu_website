package com.vn.sodu.ui;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BannerSeederTest {

    @Test
    void runInsertsOnlyMissingSeededBannerIds() {
        BannerRepo bannerRepo = mock(BannerRepo.class);
        when(bannerRepo.findAllById(any())).thenReturn(List.of(Banner.builder()
                .id(1L)
                .title("Admin edited")
                .position("home_hero_carousel")
                .build()));
        BannerSeeder seeder = new BannerSeeder(bannerRepo);

        seeder.run(null);

        ArgumentCaptor<List<Banner>> captor = ArgumentCaptor.forClass(List.class);
        verify(bannerRepo).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(21);
        assertThat(captor.getValue())
                .extracting(Banner::getId)
                .doesNotContain(1L)
                .contains(2L, 22L);
        assertThat(captor.getValue())
                .filteredOn(banner -> banner.getId().equals(22L))
                .singleElement()
                .satisfies(banner -> {
                    assertThat(banner.getPosition()).isEqualTo("home_section_04_banner");
                    assertThat(banner.getImageUrl()).contains("black-friday");
                    assertThat(banner.getDeviceType()).isEqualTo(Banner.DeviceType.ALL);
                });
    }

    @Test
    void runDoesNotSaveWhenAllSeededBannersExist() {
        BannerRepo bannerRepo = mock(BannerRepo.class);
        when(bannerRepo.findAllById(any())).thenReturn(LongStream.rangeClosed(1, 22)
                .mapToObj(id -> Banner.builder().id(id).build())
                .toList());
        BannerSeeder seeder = new BannerSeeder(bannerRepo);

        seeder.run(null);

        verify(bannerRepo, never()).saveAll(any());
    }
}
