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
