package com.vn.sodu.ui;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile({"docker", "prod"})
@RequiredArgsConstructor
public class BannerSeeder implements ApplicationRunner {

    private final BannerRepo bannerRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Long> ids = DEFAULT_BANNERS.stream()
                .map(DefaultBanner::id)
                .toList();
        Set<Long> existingIds = bannerRepo.findAllById(ids).stream()
                .map(Banner::getId)
                .collect(Collectors.toSet());

        List<Banner> missingBanners = DEFAULT_BANNERS.stream()
                .filter(banner -> !existingIds.contains(banner.id()))
                .map(this::toEntity)
                .toList();

        if (!missingBanners.isEmpty()) {
            bannerRepo.saveAll(missingBanners);
        }
    }

    private Banner toEntity(DefaultBanner banner) {
        return Banner.builder()
                .id(banner.id())
                .title(banner.title())
                .imageUrl(banner.imageUrl())
                .linkUrl(banner.linkUrl())
                .displayOrder(banner.displayOrder())
                .position(banner.position())
                .isActive(true)
                .startDate(LocalDateTime.parse("2026-05-01T00:00:00"))
                .endDate(null)
                .deviceType(banner.deviceType())
                .createdAt(banner.timestamp())
                .updatedAt(banner.timestamp())
                .build();
    }

    private record DefaultBanner(
            Long id,
            String title,
            String imageUrl,
            String linkUrl,
            Integer displayOrder,
            String position,
            Banner.DeviceType deviceType,
            LocalDateTime timestamp
    ) {
    }

    private static LocalDateTime at(String time) {
        return LocalDateTime.parse("2026-05-01T" + time);
    }

    static final List<DefaultBanner> DEFAULT_BANNERS = List.of(
            new DefaultBanner(1L, "SOBU STUDIO", "https://images.unsplash.com/photo-1614200187524-dc4b892acf16?q=80&w=2000&auto=format&fit=crop", "/products", 1, "home_hero_carousel", Banner.DeviceType.ALL, at("08:00:00")),
            new DefaultBanner(2L, "HOT WHEELS", "https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg", "/products", 2, "home_hero_carousel", Banner.DeviceType.ALL, at("08:10:00")),
            new DefaultBanner(3L, "MECHA & GUNDAM", "https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=800&auto=format&fit=crop", "/services", 3, "home_hero_carousel", Banner.DeviceType.ALL, at("08:20:00")),
            new DefaultBanner(4L, "Sidebar left promotion", "/images/banners/sidebar-best-seller.jpg", "/products", 1, "site_left_sidebar_banner", Banner.DeviceType.WEB, at("08:30:00")),
            new DefaultBanner(5L, "Sidebar right promotion", "/images/banners/sidebar-best-seller.jpg", "/products", 1, "site_right_sidebar_banner", Banner.DeviceType.WEB, at("08:40:00")),
            new DefaultBanner(6L, "Ban chay section banner", "https://i0.wp.com/www.comicbookrevolution.com/wp-content/uploads/2023/12/transformers-4-previw-banner.jpg", "/products", 1, "home_section_01_banner", Banner.DeviceType.ALL, at("08:50:00")),
            new DefaultBanner(7L, "Custom service primary", "https://images.unsplash.com/photo-1730110206448-10297c1902bd?q=80&w=800&auto=format&fit=crop", "/services", 1, "home_custom_service_image_primary", Banner.DeviceType.ALL, at("09:00:00")),
            new DefaultBanner(8L, "Custom service secondary", "https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=800&auto=format&fit=crop", "/services", 1, "home_custom_service_image_secondary", Banner.DeviceType.ALL, at("09:10:00")),
            new DefaultBanner(9L, "Custom service tertiary", "https://images.unsplash.com/photo-1532581140115-3e355d1ed1de?q=80&w=600&auto=format&fit=crop", "/services", 1, "home_custom_service_image_tertiary", Banner.DeviceType.ALL, at("09:20:00")),
            new DefaultBanner(10L, "Marvel category card", "https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg", "/category/marvel", 1, "home_category_card_01", Banner.DeviceType.ALL, at("09:30:00")),
            new DefaultBanner(11L, "DC category card", "https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg", "/category/dc", 1, "home_category_card_02", Banner.DeviceType.ALL, at("09:40:00")),
            new DefaultBanner(12L, "Hot Wheels category card", "https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg", "/category/hot wheels", 1, "home_category_card_03", Banner.DeviceType.ALL, at("09:50:00")),
            new DefaultBanner(13L, "Transformer category card", "https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg", "/category/transformer", 1, "home_category_card_04", Banner.DeviceType.ALL, at("10:00:00")),
            new DefaultBanner(14L, "Naruto category card", "https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg", "/category/naruto", 1, "home_category_card_05", Banner.DeviceType.ALL, at("10:10:00")),
            new DefaultBanner(15L, "Pacific Rim category card", "https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg", "/category/pacific rim", 1, "home_category_card_06", Banner.DeviceType.ALL, at("10:20:00")),
            new DefaultBanner(16L, "Dung Cu section banner", "https://tooltechvietnam.com/wp-content/uploads/2023/03/handtools.jpg", "/products", 1, "home_section_02_banner", Banner.DeviceType.ALL, at("10:30:00")),
            new DefaultBanner(17L, "Promo grid top left", "https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg", "/products", 1, "home_promo_grid_top_left", Banner.DeviceType.ALL, at("10:40:00")),
            new DefaultBanner(18L, "Promo grid bottom left", "https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg", "/products", 1, "home_promo_grid_bottom_left", Banner.DeviceType.ALL, at("10:50:00")),
            new DefaultBanner(19L, "Promo grid top right", "https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg", "/products", 1, "home_promo_grid_top_right", Banner.DeviceType.ALL, at("11:00:00")),
            new DefaultBanner(20L, "Promo grid bottom right", "https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg", "/products", 1, "home_promo_grid_bottom_right", Banner.DeviceType.ALL, at("11:10:00")),
            new DefaultBanner(21L, "Hotwheels section banner", "https://images.unsplash.com/photo-1551522435-a13afa10f103?q=80&w=1600&auto=format&fit=crop", "/products", 1, "home_section_03_banner", Banner.DeviceType.ALL, at("11:20:00")),
            new DefaultBanner(22L, "Sale section banner", "https://img.magnific.com/free-vector/modern-black-friday-holiday-sale-offer-banner-get-30-percent-price-drop-vector_1017-47794.jpg?semt=ais_hybrid&w=740&q=80", "/products", 1, "home_section_04_banner", Banner.DeviceType.ALL, at("11:30:00"))
    );
}
