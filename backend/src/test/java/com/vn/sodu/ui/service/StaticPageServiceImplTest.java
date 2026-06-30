package com.vn.sodu.ui.service;

import com.vn.sodu.global.dto.SearchRequest;
import com.vn.sodu.global.exception.BadRequestException;
import com.vn.sodu.global.exception.NotFoundException;
import com.vn.sodu.ui.StaticPage;
import com.vn.sodu.ui.StaticPageRepo;
import com.vn.sodu.ui.dto.StaticPageDTO;
import com.vn.sodu.ui.dto.StaticPageRequest;
import com.vn.sodu.ui.mapper.StaticPageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaticPageServiceImplTest {

    @Mock
    private StaticPageRepo staticPageRepo;

    private StaticPageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StaticPageServiceImpl(
                staticPageRepo,
                new StaticPageMapper(),
                new StaticPageHtmlSanitizer()
        );
    }

    @Test
    void createPageNormalizesSlugAndSanitizesHtml() {
        when(staticPageRepo.existsBySlug("gioi-thieu-sobu")).thenReturn(false);
        when(staticPageRepo.save(any(StaticPage.class))).thenAnswer(invocation -> {
            StaticPage page = invocation.getArgument(0);
            page.setId(1L);
            return page;
        });

        StaticPageDTO dto = service.createPage(StaticPageRequest.builder()
                .slug("Giới thiệu SOBU")
                .title(" About SOBU ")
                .htmlContent("<h1>Hi</h1><script>alert(1)</script><a href=\"javascript:bad()\" onclick=\"bad()\">bad</a>")
                .isPublished(true)
                .build());

        assertThat(dto.getSlug()).isEqualTo("gioi-thieu-sobu");
        assertThat(dto.getTitle()).isEqualTo("About SOBU");
        assertThat(dto.getHtmlContent()).contains("<h1>Hi</h1>");
        assertThat(dto.getHtmlContent()).doesNotContain("script", "onclick", "javascript:");
    }

    @Test
    void createPageRejectsDuplicateSlug() {
        when(staticPageRepo.existsBySlug("privacy-policy")).thenReturn(true);

        assertThatThrownBy(() -> service.createPage(StaticPageRequest.builder()
                .slug("Privacy Policy")
                .title("Privacy")
                .htmlContent("")
                .isPublished(true)
                .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("privacy-policy");
        verify(staticPageRepo, never()).save(any());
    }

    @Test
    void publicFetchReturnsOnlyPublishedPages() {
        StaticPage page = StaticPage.builder()
                .id(2L)
                .slug("terms")
                .title("Terms")
                .htmlContent("")
                .isPublished(true)
                .build();
        when(staticPageRepo.findBySlugAndIsPublishedTrue("terms")).thenReturn(Optional.of(page));

        StaticPageDTO dto = service.getPublishedPageBySlug("terms");

        assertThat(dto.getSlug()).isEqualTo("terms");
        verify(staticPageRepo).findBySlugAndIsPublishedTrue("terms");
    }

    @Test
    void publicFetchHidesUnpublishedPages() {
        when(staticPageRepo.findBySlugAndIsPublishedTrue("draft")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublishedPageBySlug("draft"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("draft");
    }

    @Test
    void searchPagesMapsResultsWithOneBasedPageNumber() {
        StaticPage page = StaticPage.builder()
                .id(1L)
                .slug("about")
                .title("About")
                .htmlContent("")
                .isPublished(true)
                .build();
        when(staticPageRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(page)));

        var response = service.searchPages(SearchRequest.builder()
                .searchTerm("about")
                .page(1)
                .pageSize(10)
                .sortBy("slug")
                .sortDirection("ASC")
                .build());

        assertThat(response.getPageNumber()).isEqualTo(1);
        assertThat(response.getContent()).extracting(StaticPageDTO::getSlug).containsExactly("about");
        verify(staticPageRepo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void updatePageRejectsDuplicateSlugOnAnotherPage() {
        when(staticPageRepo.findById(1L)).thenReturn(Optional.of(StaticPage.builder()
                .id(1L)
                .slug("about")
                .title("About")
                .htmlContent("")
                .isPublished(true)
                .build()));
        when(staticPageRepo.existsBySlugAndIdNot(eq("terms"), eq(1L))).thenReturn(true);

        assertThatThrownBy(() -> service.updatePage(1L, StaticPageRequest.builder()
                .slug("terms")
                .title("Terms")
                .htmlContent("")
                .isPublished(true)
                .build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("terms");
    }
}
