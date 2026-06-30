package com.vn.sodu.ui.controller;

import com.vn.sodu.ui.dto.BannerDTO;
import com.vn.sodu.ui.dto.UpdateBannerRequest;
import com.vn.sodu.ui.service.BannerService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBannerControllerTest {

    @Test
    void updateBannerDelegatesToService() {
        BannerService bannerService = mock(BannerService.class);
        AdminBannerController controller = new AdminBannerController(bannerService);
        UpdateBannerRequest request = UpdateBannerRequest.builder()
                .title("Hero")
                .imageUrl("/hero.jpg")
                .build();
        BannerDTO dto = BannerDTO.builder().id(1L).title("Hero").build();
        when(bannerService.updateBanner(1L, request, null)).thenReturn(dto);

        var response = controller.updateBanner(1L, request, null);

        verify(bannerService).updateBanner(1L, request, null);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    void adminBannerControllerDoesNotExposeCreateOrDeleteRoutes() {
        assertThat(Arrays.stream(AdminBannerController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(DeleteMapping.class)))
                .isEmpty();

        assertThat(Arrays.stream(AdminBannerController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))
                .filter(method -> {
                    PostMapping mapping = method.getAnnotation(PostMapping.class);
                    return mapping.value().length == 0 && mapping.path().length == 0;
                }))
                .isEmpty();
    }
}
