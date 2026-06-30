package com.vn.sodu.ui;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaticPageSeederTest {

    @Test
    void runSeedsDefaultPagesWhenTableIsEmpty() {
        StaticPageRepo staticPageRepo = mock(StaticPageRepo.class);
        when(staticPageRepo.count()).thenReturn(0L);
        StaticPageSeeder seeder = new StaticPageSeeder(staticPageRepo);

        seeder.run(null);

        ArgumentCaptor<List<StaticPage>> captor = ArgumentCaptor.forClass(List.class);
        verify(staticPageRepo).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(StaticPage::getSlug)
                .containsExactly("about", "privacy-policy", "terms");
        assertThat(captor.getValue())
                .allSatisfy(page -> {
                    assertThat(page.getIsPublished()).isTrue();
                    assertThat(page.getHtmlContent()).isNotBlank();
                });
    }

    @Test
    void runDoesNotReseedWhenAnyStaticPageExists() {
        StaticPageRepo staticPageRepo = mock(StaticPageRepo.class);
        when(staticPageRepo.count()).thenReturn(1L);
        StaticPageSeeder seeder = new StaticPageSeeder(staticPageRepo);

        seeder.run(null);

        verify(staticPageRepo, never()).saveAll(any());
    }
}
