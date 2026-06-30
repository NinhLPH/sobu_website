package com.vn.sodu.ui;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile({"docker", "prod"})
@RequiredArgsConstructor
public class WebsiteConfigurationSeeder implements ApplicationRunner {

    private final WebConfigRepo webConfigRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> keys = DEFAULT_CONFIGS.stream()
                .map(DefaultWebsiteConfig::key)
                .collect(Collectors.toList());
        Set<String> existingKeys = webConfigRepo.findByKeyIn(keys).stream()
                .map(WebsiteConfiguration::getKey)
                .collect(Collectors.toSet());

        List<WebsiteConfiguration> missingConfigs = DEFAULT_CONFIGS.stream()
                .filter(config -> !existingKeys.contains(config.key()))
                .map(this::toEntity)
                .collect(Collectors.toList());

        if (!missingConfigs.isEmpty()) {
            webConfigRepo.saveAll(missingConfigs);
        }
    }

    private WebsiteConfiguration toEntity(DefaultWebsiteConfig config) {
        return WebsiteConfiguration.builder()
                .key(config.key())
                .value(config.value())
                .type(config.type())
                .groupName(config.groupName())
                .isPublic(true)
                .isActive(true)
                .build();
    }

    private record DefaultWebsiteConfig(
            String key,
            String value,
            WebsiteConfiguration.ConfigType type,
            String groupName
    ) {
    }

    private static final List<DefaultWebsiteConfig> DEFAULT_CONFIGS = List.of(
            new DefaultWebsiteConfig("primary_color", "#00618e", WebsiteConfiguration.ConfigType.color, "THEME"),
            new DefaultWebsiteConfig("secondary_color", "#005f9c", WebsiteConfiguration.ConfigType.color, "THEME"),
            new DefaultWebsiteConfig("accent_color", "#5a4bb4", WebsiteConfiguration.ConfigType.color, "THEME"),
            new DefaultWebsiteConfig("background_color", "#f3f6ff", WebsiteConfiguration.ConfigType.color, "THEME"),
            new DefaultWebsiteConfig("surface_color", "#ffffff", WebsiteConfiguration.ConfigType.color, "THEME"),
            new DefaultWebsiteConfig("website_logo", "https://placehold.co/240x80?text=SOBU", WebsiteConfiguration.ConfigType.image, "THEME"),
            new DefaultWebsiteConfig("website_favicon", "/assets/favicon.png", WebsiteConfiguration.ConfigType.image, "THEME"),
            new DefaultWebsiteConfig("product_placeholder_image", "https://placehold.co/400x300?text=SOBU", WebsiteConfiguration.ConfigType.image, "THEME"),
            new DefaultWebsiteConfig("banner_placeholder_image", "https://placehold.co/1200x420?text=SOBU", WebsiteConfiguration.ConfigType.image, "THEME"),
            new DefaultWebsiteConfig("seo_default_title", "SOBU Studio - Mô hình sưu tầm & dịch vụ collector", WebsiteConfiguration.ConfigType.text, "SEO"),
            new DefaultWebsiteConfig("seo_default_description", "SOBU Studio cung cấp mô hình sưu tầm, dịch vụ đặt trước, tìm hàng và custom dành cho collector.", WebsiteConfiguration.ConfigType.text, "SEO"),
            new DefaultWebsiteConfig("seo_default_keywords", "sobu, sobu studio, mô hình sưu tầm, collector, pre-order, custom model", WebsiteConfiguration.ConfigType.text, "SEO"),
            new DefaultWebsiteConfig("seo_og_title", "SOBU Studio", WebsiteConfiguration.ConfigType.text, "SEO"),
            new DefaultWebsiteConfig("seo_og_description", "Khám phá mô hình sưu tầm, dịch vụ tìm hàng và đặt trước cùng SOBU.", WebsiteConfiguration.ConfigType.text, "SEO"),
            new DefaultWebsiteConfig("seo_og_image", "https://placehold.co/1200x630?text=SOBU+Studio", WebsiteConfiguration.ConfigType.image, "SEO"),
            new DefaultWebsiteConfig("seo_robots_index_enabled", "true", WebsiteConfiguration.ConfigType.boolean_type, "SEO"),
            new DefaultWebsiteConfig("site_name", "SOBU", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("store_display_name", "SOBU Studio", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("support_hotline", "1900 636 999", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("support_email", "support@sobu.vn", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("company_name", "SOBU Studio", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("company_address", "Hà Nam, Việt Nam", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("working_hours", "09:00 - 21:00, Thứ 2 - Chủ nhật", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("footer_greeting_text", "SOBU đồng hành cùng cộng đồng collector trong từng đơn hàng và yêu cầu đặc biệt.", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("maintenance_mode_enabled", "false", WebsiteConfiguration.ConfigType.boolean_type, "GENERAL"),
            new DefaultWebsiteConfig("maintenance_message", "Website đang được bảo trì. Vui lòng quay lại sau.", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("social_links", "{\"facebook\":\"\",\"instagram\":\"\",\"tiktok\":\"\",\"youtube\":\"\",\"zalo\":\"\"}", WebsiteConfiguration.ConfigType.json, "SOCIAL"),
            new DefaultWebsiteConfig("social_share_enabled", "true", WebsiteConfiguration.ConfigType.boolean_type, "SOCIAL"),
            new DefaultWebsiteConfig("social_chat_widget_enabled", "false", WebsiteConfiguration.ConfigType.boolean_type, "SOCIAL"),
            new DefaultWebsiteConfig("social_chat_config", "{\"provider\":\"zalo\",\"pageId\":\"\",\"greetingText\":\"SOBU có thể hỗ trợ gì cho bạn?\"}", WebsiteConfiguration.ConfigType.json, "SOCIAL"),
            new DefaultWebsiteConfig("copyright_text", "(c) 2026 SOBU Studio. All rights reserved.", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("newsletter_enabled", "false", WebsiteConfiguration.ConfigType.boolean_type, "GENERAL"),
            new DefaultWebsiteConfig("newsletter_description", "Nhan thong tin ve san pham moi, hang sap ve va uu dai rieng cho collector.", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("newsletter_submit_label", "Dang ky", WebsiteConfiguration.ConfigType.text, "GENERAL"),
            new DefaultWebsiteConfig("footer_company_links", "[{\"label\":\"Gioi thieu\",\"href\":\"/about\"},{\"label\":\"Dich vu\",\"href\":\"/services\"},{\"label\":\"Blog\",\"href\":\"/blog\"}]", WebsiteConfiguration.ConfigType.json, "FOOTER"),
            new DefaultWebsiteConfig("footer_help_links", "[{\"label\":\"San pham\",\"href\":\"/products\"},{\"label\":\"Yeu cau tim hang\",\"href\":\"/request\"},{\"label\":\"Lien he\",\"href\":\"/contact\"}]", WebsiteConfiguration.ConfigType.json, "FOOTER"),
            new DefaultWebsiteConfig("legal_links", "[{\"label\":\"Dieu khoan\",\"href\":\"/terms\"},{\"label\":\"Bao mat\",\"href\":\"/privacy\"}]", WebsiteConfiguration.ConfigType.json, "FOOTER"),
            new DefaultWebsiteConfig("home_section_01_title", "BAN CHAY", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_01_subtitle", "Giao Hang Toan Quoc", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_01_label", "Xem them", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_01_cta_url", "/products", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_custom_service_title", "DICH VU DO MO HINH SO 1 VIET NAM", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_custom_service_badges", "[\"Do Led cam ung\",\"Son mo hinh chuan phim\",\"Custom theo y thich\"]", WebsiteConfiguration.ConfigType.json, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_custom_service_cta_label", "CUSTOM NGAY", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_custom_service_cta_url", "/services", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_02_title", "MO HINH CUSTOM", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_02_subtitle", "Giao Hang Toan Quoc", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_02_cta_label", "Xem them", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_02_cta_url", "/products", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_category_title", "The loai mo hinh", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_category_cards", "[{\"label\":\"Marvel\",\"href\":\"/category/marvel\",\"bannerPosition\":\"home_category_card_01\"},{\"label\":\"DC\",\"href\":\"/category/dc\",\"bannerPosition\":\"home_category_card_02\"},{\"label\":\"Hot Wheels\",\"href\":\"/category/hot wheels\",\"bannerPosition\":\"home_category_card_03\"},{\"label\":\"Transformer\",\"href\":\"/category/transformer\",\"bannerPosition\":\"home_category_card_04\"},{\"label\":\"Naruto\",\"href\":\"/category/naruto\",\"bannerPosition\":\"home_category_card_05\"},{\"label\":\"Pacific Rim\",\"href\":\"/category/pacific rim\",\"bannerPosition\":\"home_category_card_06\"}]", WebsiteConfiguration.ConfigType.json, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_03_title", "Dung Cu", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_03_subtitle", "Giao Hang Toan Quoc", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_03_cta_label", "Xem them", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_03_cta_url", "/products", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_04_title", "Hotwheels", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_04_subtitle", "Giao Hang Toan Quoc", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_04_cta_label", "Xem them", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_04_cta_url", "/products", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_05_title", "Giam gia cuc manh", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_05_subtitle", "", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_05_cta_label", "Xem them", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_section_05_cta_url", "/products", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_news_title", "Tin Tuc", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_news_more_label", "MORE", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_news_more_url", "/blog", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_testimonials_title", "Danh gia tu khach hang", WebsiteConfiguration.ConfigType.text, "HOME_SECTION"),
            new DefaultWebsiteConfig("home_promo_grid_top_left_title", "HOT WHEELS", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_top_left_description", "KHAM PHA NHUNG MAU XE MO HINH HOT NHAT DANH CHO NGUOI DAM ME TOC DO.", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_top_left_cta_label", "Xem them", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_top_left_cta_url", "/products", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_bottom_left_title", "Suu tam huyen thoai", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_bottom_left_description", "SUU TAM NHUNG MAU XE HUYEN THOAI - TU SIEU XE HIEN DAI DEN CLASSIC CO DIEN.", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_bottom_left_cta_label", "", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_bottom_left_cta_url", "/products", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_top_right_title", "Limited Edition Cars", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_top_right_description", "DISCOVER LIMITED EDITION CARS AND EXCLUSIVE RELEASES FOR TRUE COLLECTORS.", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_top_right_cta_label", "", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_top_right_cta_url", "/products", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_bottom_right_title", "GIFT FOR COLLECTORS", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_bottom_right_description", "MON QUA HOAN HAO CHO NGUOI YEU XE VA DAM ME MO HINH.", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_bottom_right_cta_label", "Xem them", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_promo_grid_bottom_right_cta_url", "/products", WebsiteConfiguration.ConfigType.text, "HOME_PROMO"),
            new DefaultWebsiteConfig("home_partners_title", "Doi tac chien luoc & Thuong hieu dong hanh", WebsiteConfiguration.ConfigType.text, "HOME_PARTNER"),
            new DefaultWebsiteConfig("home_partner_brands", "[{\"name\":\"BANDAI\",\"logoUrl\":\"https://placehold.co/180x60/e60012/ffffff?text=BANDAI\"},{\"name\":\"HOT TOYS\",\"logoUrl\":\"https://placehold.co/180x60/111111/f1b82d?text=HOT+TOYS\"},{\"name\":\"TAMIYA\",\"logoUrl\":\"https://placehold.co/180x60/0054a6/ffffff?text=TAMIYA\"},{\"name\":\"LEGO\",\"logoUrl\":\"https://placehold.co/180x60/ffd500/000000?text=LEGO\"},{\"name\":\"MATTEL\",\"logoUrl\":\"https://placehold.co/180x60/e5142a/ffffff?text=MATTEL\"},{\"name\":\"HASBRO\",\"logoUrl\":\"https://placehold.co/180x60/0072ce/ffffff?text=HASBRO\"}]", WebsiteConfiguration.ConfigType.json, "HOME_PARTNER"),
            new DefaultWebsiteConfig("free_shipping_threshold", "500000", WebsiteConfiguration.ConfigType.number, "CHECKOUT"),
            new DefaultWebsiteConfig("max_cart_items", "99", WebsiteConfiguration.ConfigType.number, "CHECKOUT"),
            new DefaultWebsiteConfig("checkout_guest_checkout_enabled", "false", WebsiteConfiguration.ConfigType.boolean_type, "CHECKOUT"),
            new DefaultWebsiteConfig("checkout_cod_enabled", "true", WebsiteConfiguration.ConfigType.boolean_type, "CHECKOUT"),
            new DefaultWebsiteConfig("checkout_online_payment_enabled", "true", WebsiteConfiguration.ConfigType.boolean_type, "CHECKOUT"),
            new DefaultWebsiteConfig("checkout_default_payment_method", "ONLINE", WebsiteConfiguration.ConfigType.text, "CHECKOUT"),
            new DefaultWebsiteConfig("checkout_shipping_fee_default", "0", WebsiteConfiguration.ConfigType.number, "CHECKOUT"),
            new DefaultWebsiteConfig("checkout_order_note_enabled", "true", WebsiteConfiguration.ConfigType.boolean_type, "CHECKOUT"),
            new DefaultWebsiteConfig("business_currency", "VND", WebsiteConfiguration.ConfigType.text, "BUSINESS"),
            new DefaultWebsiteConfig("business_vat_rate", "10", WebsiteConfiguration.ConfigType.number, "BUSINESS"),
            new DefaultWebsiteConfig("business_inventory_hold_minutes", "15", WebsiteConfiguration.ConfigType.number, "BUSINESS"),
            new DefaultWebsiteConfig("business_order_auto_cancel_minutes", "30", WebsiteConfiguration.ConfigType.number, "BUSINESS"),
            new DefaultWebsiteConfig("business_return_period_days", "7", WebsiteConfiguration.ConfigType.number, "BUSINESS"),
            new DefaultWebsiteConfig("business_exchange_period_days", "7", WebsiteConfiguration.ConfigType.number, "BUSINESS"),
            new DefaultWebsiteConfig("business_low_stock_threshold", "5", WebsiteConfiguration.ConfigType.number, "BUSINESS")
    );
}
