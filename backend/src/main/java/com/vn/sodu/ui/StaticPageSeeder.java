package com.vn.sodu.ui;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile({"docker", "prod"})
@RequiredArgsConstructor
public class StaticPageSeeder implements ApplicationRunner {

    private final StaticPageRepo staticPageRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (staticPageRepo.count() > 0) {
            return;
        }

        staticPageRepo.saveAll(DEFAULT_PAGES.stream()
                .map(this::toEntity)
                .toList());
    }

    private StaticPage toEntity(DefaultStaticPage page) {
        return StaticPage.builder()
                .id(page.id())
                .slug(page.slug())
                .title(page.title())
                .htmlContent(page.htmlContent())
                .isPublished(true)
                .build();
    }

    private record DefaultStaticPage(
            Long id,
            String slug,
            String title,
            String htmlContent
    ) {
    }

    static final List<DefaultStaticPage> DEFAULT_PAGES = List.of(
            new DefaultStaticPage(1L, "about", "About", "<h1>About SOBU</h1><p>SOBU Studio serves collectors with model products, preorder support, sourcing, and custom services.</p>"),
            new DefaultStaticPage(2L, "privacy-policy", "Privacy Policy", "<h1>Privacy Policy</h1><p>SOBU collects only the information needed to process orders, support requests, and improve customer service.</p>"),
            new DefaultStaticPage(3L, "terms", "Terms", "<h1>Terms</h1><p>By using SOBU services, customers agree to provide accurate order information and follow the published payment and delivery policies.</p>")
    );
}
