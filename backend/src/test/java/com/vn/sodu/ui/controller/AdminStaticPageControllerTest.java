package com.vn.sodu.ui.controller;

import com.vn.sodu.ui.dto.StaticPageDTO;
import com.vn.sodu.ui.dto.StaticPageRequest;
import com.vn.sodu.ui.service.StaticPageService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminStaticPageControllerTest {

    @Test
    void createPageAllowsStaffAuthentication() {
        StaticPageService service = mock(StaticPageService.class);
        AdminStaticPageController controller = new AdminStaticPageController(service);
        StaticPageRequest request = StaticPageRequest.builder()
                .slug("about")
                .title("About")
                .htmlContent("")
                .isPublished(true)
                .build();
        StaticPageDTO dto = StaticPageDTO.builder().id(1L).slug("about").title("About").build();
        when(service.createPage(request)).thenReturn(dto);

        var response = controller.createPage(staffAuth(), request);

        verify(service).createPage(request);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(dto);
    }

    @Test
    void searchPagesRejectsNonStaffAuthentication() {
        StaticPageService service = mock(StaticPageService.class);
        AdminStaticPageController controller = new AdminStaticPageController(service);

        assertThatThrownBy(() -> controller.searchPages(userAuth(), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private TestingAuthenticationToken staffAuth() {
        return new TestingAuthenticationToken("staff@sobu.vn", null, "ROLE_STAFF");
    }

    private TestingAuthenticationToken userAuth() {
        return new TestingAuthenticationToken("user@sobu.vn", null, "ROLE_USER");
    }
}
