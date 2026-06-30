package com.vn.sodu.ui.mapper;

import com.vn.sodu.ui.Banner;
import com.vn.sodu.ui.dto.BannerDTO;
import com.vn.sodu.ui.dto.CreateBannerRequest;
import com.vn.sodu.ui.dto.UpdateBannerRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BannerMapperTest {

    private final BannerMapper mapper = new BannerMapper();

    @Test
    void toEntityPreservesDynamicPositionString() {
        CreateBannerRequest request = CreateBannerRequest.builder()
                .title("Checkout promo")
                .imageUrl("https://domain.com/image.png")
                .position("checkout_top")
                .deviceType(Banner.DeviceType.WEB)
                .build();

        Banner banner = mapper.toEntity(request);

        assertThat(banner.getPosition()).isEqualTo("checkout_top");
    }

    @Test
    void toDtoPreservesNullPosition() {
        Banner banner = Banner.builder()
                .id(1L)
                .title("Generic promo")
                .imageUrl("https://domain.com/image.png")
                .position(null)
                .build();

        BannerDTO dto = mapper.toDTO(banner);

        assertThat(dto.getPosition()).isNull();
    }

    @Test
    void updateEntityKeepsSeededPositionAndUpdatesEditableFields() {
        Banner banner = Banner.builder()
                .title("Existing promo")
                .imageUrl("https://domain.com/image.png")
                .position("home_top")
                .isActive(true)
                .startDate(LocalDateTime.parse("2026-05-01T00:00:00"))
                .endDate(LocalDateTime.parse("2026-06-01T00:00:00"))
                .build();
        UpdateBannerRequest request = UpdateBannerRequest.builder()
                .title("Updated promo")
                .position("category_shoes_top")
                .isActive(false)
                .startDate(null)
                .endDate(null)
                .build();

        mapper.updateEntity(banner, request);

        assertThat(banner.getTitle()).isEqualTo("Updated promo");
        assertThat(banner.getPosition()).isEqualTo("home_top");
        assertThat(banner.getIsActive()).isFalse();
        assertThat(banner.getStartDate()).isNull();
        assertThat(banner.getEndDate()).isNull();
    }
}
