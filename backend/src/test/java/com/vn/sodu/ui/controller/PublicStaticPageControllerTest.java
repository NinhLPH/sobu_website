package com.vn.sodu.ui.controller;

import com.vn.sodu.ui.dto.StaticPageDTO;
import com.vn.sodu.ui.service.StaticPageService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicStaticPageControllerTest {

    @Test
    void getPageBySlugDelegatesToPublishedLookup() {
        StaticPageService service = mock(StaticPageService.class);
        PublicStaticPageController controller = new PublicStaticPageController(service);
        StaticPageDTO dto = StaticPageDTO.builder()
                .id(1L)
                .slug("privacy-policy")
                .title("Privacy Policy")
                .htmlContent("")
                .isPublished(true)
                .build();
        when(service.getPublishedPageBySlug("privacy-policy")).thenReturn(dto);

        var response = controller.getPageBySlug("privacy-policy");

        verify(service).getPublishedPageBySlug("privacy-policy");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }
}
