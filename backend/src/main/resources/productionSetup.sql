-- ========================================================
-- SOBU PRODUCTION INITIAL SETUP SCRIPT
-- Purpose: Initialize foundational system data for Production
-- (Roles, Admin Account, Loyalty System, System Configurations, Static Pages, Default Banners)
-- Safe & Idempotent: Can be executed multiple times without data duplication.
-- ========================================================

SET NAMES 'utf8mb4';
SET CHARACTER SET utf8mb4;

-- Grant full privileges to root from any host (covers Docker bridge IPs like 172.18.0.1)
-- Run this block as a privileged session if the error persists:
-- GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
-- FLUSH PRIVILEGES;

-- 1. SYSTEM ROLES
INSERT INTO role (name, description) VALUES 
('ADMIN', 'Quản trị viên hệ thống'),
('USER', 'Khách hàng'),
('MANAGER', 'Quản lý'),
('STAFF', 'Nhân viên kho và đơn hàng')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 2. INITIAL ADMIN ACCOUNT
-- Default login: admin@sobu.vn / password (BCrypt hash)
-- NOTE: Please update the password immediately after first login in production!
INSERT INTO account (id, role_id, email, phone, password_hash, full_name, status) VALUES
(1, (SELECT id FROM role WHERE name = 'ADMIN'), 'admin@sobu.vn', '0901000001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Quản trị viên SOBU', 'ACTIVE')
ON DUPLICATE KEY UPDATE
role_id = VALUES(role_id),
full_name = VALUES(full_name),
status = VALUES(status);

-- 3. LOYALTY TIERS & RULES
INSERT INTO loyalty_tiers (id, name, min_total_money, discount_rate) VALUES
(1, 0, 0, 0),          -- Đồng (Bronze): 0%
(2, 1, 5000000, 3),    -- Bạc (Silver): >= 5.000.000 VNĐ -> 3%
(3, 2, 15000000, 7)    -- Vàng (Gold): >= 15.000.000 VNĐ -> 7%
ON DUPLICATE KEY UPDATE
name = VALUES(name),
min_total_money = VALUES(min_total_money),
discount_rate = VALUES(discount_rate);

INSERT INTO loyalty_rules (id, code, `value`, active) VALUES
(1, 'EARN_RATE', '10000:1', true),     -- 10.000 VNĐ = 1 điểm
(2, 'REDEEM_RATE', '1:1000', true),    -- 1 điểm = 1.000 VNĐ
(3, 'BIRTHDAY_BONUS', '200', true)     -- 200 điểm sinh nhật
ON DUPLICATE KEY UPDATE
code = VALUES(code),
`value` = VALUES(`value`),
active = VALUES(active);

-- 4. STATIC PAGES
INSERT INTO static_pages (
id, slug, title, html_content, is_published, created_at, updated_at
) VALUES
(1, 'about', 'Giới thiệu', '<h1>Về SOBU Studio</h1><p>SOBU Studio chuyên cung cấp mô hình sưu tầm chính hãng, dịch vụ đặt hàng trước (pre-order), tìm kiếm mô hình hiếm và custom sơn/độ LED theo yêu cầu.</p>', true, NOW(), NOW()),
(2, 'privacy-policy', 'Chính sách bảo mật', '<h1>Chính sách bảo mật</h1><p>SOBU cam kết bảo mật thông tin cá nhân của khách hàng và chỉ sử dụng cho mục đích xử lý đơn hàng và chăm sóc khách hàng.</p>', true, NOW(), NOW()),
(3, 'terms', 'Điều khoản sử dụng', '<h1>Điều khoản dịch vụ</h1><p>Bằng việc sử dụng website và dịch vụ của SOBU Studio, quý khách đồng ý tuân thủ các quy định đặt hàng, thanh toán và vận chuyển được công bố công khai.</p>', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
title = VALUES(title),
html_content = VALUES(html_content),
is_published = VALUES(is_published),
updated_at = NOW();

-- 5. WEBSITE CONFIGURATIONS
INSERT INTO website_configurations (
config_key, config_value, type, group_name, description, is_public, is_active, created_at, updated_at
) VALUES
-- Theme Colors & Branding
('primary_color', '#00618e', 'color', 'THEME', 'Màu sắc chủ đạo', true, true, NOW(), NOW()),
('secondary_color', '#005f9c', 'color', 'THEME', 'Màu phụ', true, true, NOW(), NOW()),
('accent_color', '#5a4bb4', 'color', 'THEME', 'Màu nhấn', true, true, NOW(), NOW()),
('background_color', '#f3f6ff', 'color', 'THEME', 'Màu nền trang web', true, true, NOW(), NOW()),
('surface_color', '#ffffff', 'color', 'THEME', 'Màu bề mặt card/container', true, true, NOW(), NOW()),
('website_logo', 'https://placehold.co/240x80?text=SOBU', 'image', 'THEME', 'Logo thương hiệu', true, true, NOW(), NOW()),
('website_favicon', '/assets/favicon.png', 'image', 'THEME', 'Favicon', true, true, NOW(), NOW()),
('product_placeholder_image', 'https://placehold.co/400x300?text=SOBU', 'image', 'THEME', 'Ảnh placeholder sản phẩm', true, true, NOW(), NOW()),
('banner_placeholder_image', 'https://placehold.co/1200x420?text=SOBU', 'image', 'THEME', 'Ảnh placeholder banner', true, true, NOW(), NOW()),

-- SEO
('seo_default_title', 'SOBU Studio - Mô hình sưu tầm & dịch vụ collector', 'text', 'SEO', 'Tiêu đề mặc định', true, true, NOW(), NOW()),
('seo_default_description', 'SOBU Studio cung cấp mô hình sưu tầm, dịch vụ đặt trước, tìm hàng và custom chuyên nghiệp dành cho collector.', 'text', 'SEO', 'Mô tả mặc định', true, true, NOW(), NOW()),
('seo_default_keywords', 'sobu, sobu studio, mô hình sưu tầm, collector, pre-order, custom model, gundam, hot wheels', 'text', 'SEO', 'Từ khóa mặc định', true, true, NOW(), NOW()),
('seo_og_title', 'SOBU Studio', 'text', 'SEO', 'OG Title mạng xã hội', true, true, NOW(), NOW()),
('seo_og_description', 'Khám phá thế giới mô hình sưu tầm và dịch vụ custom chuyên nghiệp tại SOBU Studio.', 'text', 'SEO', 'OG Description', true, true, NOW(), NOW()),
('seo_og_image', 'https://placehold.co/1200x630?text=SOBU+Studio', 'image', 'SEO', 'Ảnh chia sẻ mạng xã hội', true, true, NOW(), NOW()),
('seo_robots_index_enabled', 'true', 'boolean_type', 'SEO', 'Cho phép bot tìm kiếm index', true, true, NOW(), NOW()),

-- General Store Information
('site_name', 'SOBU', 'text', 'GENERAL', 'Tên website', true, true, NOW(), NOW()),
('store_display_name', 'SOBU Studio', 'text', 'GENERAL', 'Tên hiển thị cửa hàng', true, true, NOW(), NOW()),
('support_hotline', '1900 636 999', 'text', 'GENERAL', 'Hotline hỗ trợ', true, true, NOW(), NOW()),
('support_email', 'support@sobu.vn', 'text', 'GENERAL', 'Email hỗ trợ', true, true, NOW(), NOW()),
('company_name', 'SOBU Studio', 'text', 'GENERAL', 'Tên công ty / thương hiệu', true, true, NOW(), NOW()),
('company_address', 'Hà Nam, Việt Nam', 'text', 'GENERAL', 'Địa chỉ trụ sở', true, true, NOW(), NOW()),
('working_hours', '09:00 - 21:00, Thứ 2 - Chủ nhật', 'text', 'GENERAL', 'Giờ làm việc', true, true, NOW(), NOW()),
('footer_greeting_text', 'SOBU đồng hành cùng cộng đồng collector trong từng mô hình và dịch vụ đặc biệt.', 'text', 'GENERAL', 'Lời chào chân trang', true, true, NOW(), NOW()),
('copyright_text', '© 2026 SOBU Studio. All rights reserved.', 'text', 'GENERAL', 'Bản quyền', true, true, NOW(), NOW()),
('maintenance_mode_enabled', 'false', 'boolean_type', 'GENERAL', 'Chế độ bảo trì', true, true, NOW(), NOW()),
('maintenance_message', 'Website đang được bảo trì. Vui lòng quay lại sau.', 'text', 'GENERAL', 'Thông báo bảo trì', true, true, NOW(), NOW()),
('newsletter_enabled', 'false', 'boolean_type', 'GENERAL', 'Bật form nhận bản tin', true, true, NOW(), NOW()),
('newsletter_description', 'Nhận thông tin về mô hình mới, hàng sắp về và ưu đãi dành riêng cho collector.', 'text', 'GENERAL', 'Mô tả nhận bản tin', true, true, NOW(), NOW()),
('newsletter_submit_label', 'Đăng ký', 'text', 'GENERAL', 'Nhãn nút đăng ký bản tin', true, true, NOW(), NOW()),

-- Navigation & Footer Links
('footer_company_links', '[{"label":"Giới thiệu","href":"/about"},{"label":"Dịch vụ","href":"/services"},{"label":"Tin tức","href":"/blog"}]', 'json', 'FOOTER', 'Liên kết công ty', true, true, NOW(), NOW()),
('footer_help_links', '[{"label":"Sản phẩm","href":"/products"},{"label":"Yêu cầu tìm hàng","href":"/request"},{"label":"Liên hệ","href":"/contact"}]', 'json', 'FOOTER', 'Liên kết hỗ trợ', true, true, NOW(), NOW()),
('legal_links', '[{"label":"Điều khoản","href":"/terms"},{"label":"Bảo mật","href":"/privacy"}]', 'json', 'FOOTER', 'Liên kết pháp lý', true, true, NOW(), NOW()),

-- Home Sections UI Config
('home_section_01_title', 'BÁN CHẠY', 'text', 'HOME_SECTION', 'Tiêu đề khu vực bán chạy', true, true, NOW(), NOW()),
('home_section_01_subtitle', 'Giao Hàng Toàn Quốc', 'text', 'HOME_SECTION', 'Phụ đề khu vực bán chạy', true, true, NOW(), NOW()),
('home_section_01_label', 'Xem thêm', 'text', 'HOME_SECTION', 'Nhãn nút', true, true, NOW(), NOW()),
('home_section_01_cta_url', '/products', 'text', 'HOME_SECTION', 'Đường dẫn xem thêm', true, true, NOW(), NOW()),
('home_custom_service_title', 'DỊCH VỤ ĐỘ MÔ HÌNH SỐ 1 VIỆT NAM', 'text', 'HOME_SECTION', 'Tiêu đề dịch vụ custom', true, true, NOW(), NOW()),
('home_custom_service_badges', '["Độ Led cảm ứng","Sơn mô hình chuẩn phim","Custom theo ý thích"]', 'json', 'HOME_SECTION', 'Huy hiệu dịch vụ', true, true, NOW(), NOW()),
('home_custom_service_cta_label', 'CUSTOM NGAY', 'text', 'HOME_SECTION', 'Nhãn nút đặt custom', true, true, NOW(), NOW()),
('home_custom_service_cta_url', '/services', 'text', 'HOME_SECTION', 'Đường dẫn dịch vụ custom', true, true, NOW(), NOW()),
('home_section_02_title', 'MÔ HÌNH CUSTOM', 'text', 'HOME_SECTION', 'Tiêu đề mục mô hình custom', true, true, NOW(), NOW()),
('home_section_02_subtitle', 'Giao Hàng Toàn Quốc', 'text', 'HOME_SECTION', 'Phụ đề', true, true, NOW(), NOW()),
('home_section_02_cta_label', 'Xem thêm', 'text', 'HOME_SECTION', 'Nhãn nút', true, true, NOW(), NOW()),
('home_section_02_cta_url', '/products', 'text', 'HOME_SECTION', 'Đường dẫn', true, true, NOW(), NOW()),
('home_category_title', 'Thể loại mô hình', 'text', 'HOME_SECTION', 'Tiêu đề danh mục trang chủ', true, true, NOW(), NOW()),
('home_category_cards', '[{"label":"Marvel","href":"/category/marvel","bannerPosition":"home_category_card_01"},{"label":"DC","href":"/category/dc","bannerPosition":"home_category_card_02"},{"label":"Hot Wheels","href":"/category/hot wheels","bannerPosition":"home_category_card_03"},{"label":"Transformer","href":"/category/transformer","bannerPosition":"home_category_card_04"},{"label":"Naruto","href":"/category/naruto","bannerPosition":"home_category_card_05"},{"label":"Pacific Rim","href":"/category/pacific rim","bannerPosition":"home_category_card_06"}]', 'json', 'HOME_SECTION', 'Cấu hình thẻ danh mục', true, true, NOW(), NOW()),
('home_section_03_title', 'Dụng Cụ', 'text', 'HOME_SECTION', 'Tiêu đề mục dụng cụ', true, true, NOW(), NOW()),
('home_section_03_subtitle', 'Giao Hàng Toàn Quốc', 'text', 'HOME_SECTION', 'Phụ đề mục dụng cụ', true, true, NOW(), NOW()),
('home_section_03_cta_label', 'Xem thêm', 'text', 'HOME_SECTION', 'Nhãn nút', true, true, NOW(), NOW()),
('home_section_03_cta_url', '/products', 'text', 'HOME_SECTION', 'Đường dẫn', true, true, NOW(), NOW()),
('home_section_04_title', 'Hot Wheels', 'text', 'HOME_SECTION', 'Tiêu đề mục Hot Wheels', true, true, NOW(), NOW()),
('home_section_04_subtitle', 'Giao Hàng Toàn Quốc', 'text', 'HOME_SECTION', 'Phụ đề', true, true, NOW(), NOW()),
('home_section_04_cta_label', 'Xem thêm', 'text', 'HOME_SECTION', 'Nhãn nút', true, true, NOW(), NOW()),
('home_section_04_cta_url', '/products', 'text', 'HOME_SECTION', 'Đường dẫn', true, true, NOW(), NOW()),
('home_section_05_title', 'Giảm giá cực mạnh', 'text', 'HOME_SECTION', 'Tiêu đề mục khuyến mãi', true, true, NOW(), NOW()),
('home_section_05_subtitle', 'Ưu Đãi Có Hạn', 'text', 'HOME_SECTION', 'Phụ đề khuyến mãi', true, true, NOW(), NOW()),
('home_section_05_cta_label', 'Xem thêm', 'text', 'HOME_SECTION', 'Nhãn nút', true, true, NOW(), NOW()),
('home_section_05_cta_url', '/products', 'text', 'HOME_SECTION', 'Đường dẫn', true, true, NOW(), NOW()),
('home_news_title', 'Tin Tức', 'text', 'HOME_SECTION', 'Tiêu đề tin tức', true, true, NOW(), NOW()),
('home_news_more_label', 'XEM TẤT CẢ', 'text', 'HOME_SECTION', 'Nhãn xem thêm tin tức', true, true, NOW(), NOW()),
('home_news_more_url', '/blog', 'text', 'HOME_SECTION', 'Đường dẫn trang tin tức', true, true, NOW(), NOW()),
('home_testimonials_title', 'Đánh giá từ khách hàng', 'text', 'HOME_SECTION', 'Tiêu đề đánh giá khách hàng', true, true, NOW(), NOW()),
('home_promo_grid_top_left_title', 'HOT WHEELS', 'text', 'HOME_PROMO', 'Tiêu đề banner promo trên trái', true, true, NOW(), NOW()),
('home_promo_grid_top_left_description', 'KHÁM PHÁ NHỮNG MẪU XE MÔ HÌNH HOT NHẤT DÀNH CHO NGƯỜI ĐAM MÊ TỐC ĐỘ.', 'text', 'HOME_PROMO', 'Mô tả', true, true, NOW(), NOW()),
('home_promo_grid_top_left_cta_label', 'Xem thêm', 'text', 'HOME_PROMO', 'Nhãn nút', true, true, NOW(), NOW()),
('home_promo_grid_top_left_cta_url', '/products', 'text', 'HOME_PROMO', 'Đường dẫn', true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_title', 'Sưu tầm huyền thoại', 'text', 'HOME_PROMO', 'Tiêu đề promo dưới trái', true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_description', 'SƯU TẦM NHỮNG MẪU XE HUYỀN THOẠI - TỪ SIÊU XE HIỆN ĐẠI ĐẾN CLASSIC CỔ ĐIỂN.', 'text', 'HOME_PROMO', 'Mô tả', true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_cta_label', '', 'text', 'HOME_PROMO', 'Nhãn nút', true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_cta_url', '/products', 'text', 'HOME_PROMO', 'Đường dẫn', true, true, NOW(), NOW()),
('home_promo_grid_top_right_title', 'Limited Edition Cars', 'text', 'HOME_PROMO', 'Tiêu đề promo trên phải', true, true, NOW(), NOW()),
('home_promo_grid_top_right_description', 'DISCOVER LIMITED EDITION CARS AND EXCLUSIVE RELEASES FOR TRUE COLLECTORS.', 'text', 'HOME_PROMO', 'Mô tả', true, true, NOW(), NOW()),
('home_promo_grid_top_right_cta_label', '', 'text', 'HOME_PROMO', 'Nhãn nút', true, true, NOW(), NOW()),
('home_promo_grid_top_right_cta_url', '/products', 'text', 'HOME_PROMO', 'Đường dẫn', true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_title', 'GIFT FOR COLLECTORS', 'text', 'HOME_PROMO', 'Tiêu đề promo dưới phải', true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_description', 'MÓN QUÀ HOÀN HẢO CHO NGƯỜI YÊU XE VÀ ĐAM MÊ MÔ HÌNH.', 'text', 'HOME_PROMO', 'Mô tả', true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_cta_label', 'Xem thêm', 'text', 'HOME_PROMO', 'Nhãn nút', true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_cta_url', '/products', 'text', 'HOME_PROMO', 'Đường dẫn', true, true, NOW(), NOW()),
('home_partners_title', 'Đối tác chiến lược & Thương hiệu đồng hành', 'text', 'HOME_PARTNER', 'Tiêu đề đối tác', true, true, NOW(), NOW()),
('home_partner_brands', '[{"name":"BANDAI","logoUrl":"https://placehold.co/180x60/e60012/ffffff?text=BANDAI"},{"name":"HOT TOYS","logoUrl":"https://placehold.co/180x60/111111/f1b82d?text=HOT+TOYS"},{"name":"TAMIYA","logoUrl":"https://placehold.co/180x60/0054a6/ffffff?text=TAMIYA"},{"name":"LEGO","logoUrl":"https://placehold.co/180x60/ffd500/000000?text=LEGO"},{"name":"MATTEL","logoUrl":"https://placehold.co/180x60/e5142a/ffffff?text=MATTEL"},{"name":"HASBRO","logoUrl":"https://placehold.co/180x60/0072ce/ffffff?text=HASBRO"}]', 'json', 'HOME_PARTNER', 'Danh sách thương hiệu', true, true, NOW(), NOW()),

-- Social Links
('social_links', '{"facebook":"https://facebook.com","instagram":"","tiktok":"","youtube":"","zalo":""}', 'json', 'SOCIAL', 'Liên kết mạng xã hội', true, true, NOW(), NOW()),
('social_share_enabled', 'true', 'boolean_type', 'SOCIAL', 'Bật chia sẻ MXH', true, true, NOW(), NOW()),
('social_chat_widget_enabled', 'false', 'boolean_type', 'SOCIAL', 'Bật widget chat', true, true, NOW(), NOW()),
('social_chat_config', '{"provider":"zalo","pageId":"","greetingText":"SOBU có thể hỗ trợ gì cho bạn?"}', 'json', 'SOCIAL', 'Cấu hình chat widget', true, true, NOW(), NOW()),

-- Checkout & Business Policies
('free_shipping_threshold', '500000', 'number', 'CHECKOUT', 'Ngưỡng miễn phí vận chuyển', true, true, NOW(), NOW()),
('max_cart_items', '99', 'number', 'CHECKOUT', 'Số lượng tối đa trong giỏ', true, true, NOW(), NOW()),
('checkout_guest_checkout_enabled', 'false', 'boolean_type', 'CHECKOUT', 'Cho phép mua hàng không cần đăng nhập', true, true, NOW(), NOW()),
('checkout_cod_enabled', 'true', 'boolean_type', 'CHECKOUT', 'Bật phương thức COD', true, true, NOW(), NOW()),
('checkout_online_payment_enabled', 'true', 'boolean_type', 'CHECKOUT', 'Bật thanh toán online qua PayOS', true, true, NOW(), NOW()),
('checkout_default_payment_method', 'ONLINE', 'text', 'CHECKOUT', 'Phương thức thanh toán mặc định', true, true, NOW(), NOW()),
('checkout_shipping_fee_default', '0', 'number', 'CHECKOUT', 'Phí vận chuyển mặc định', true, true, NOW(), NOW()),
('checkout_order_note_enabled', 'true', 'boolean_type', 'CHECKOUT', 'Cho phép ghi chú đơn hàng', true, true, NOW(), NOW()),
('business_currency', 'VND', 'text', 'BUSINESS', 'Đơn vị tiền tệ', true, true, NOW(), NOW()),
('business_vat_rate', '10', 'number', 'BUSINESS', 'Thuế VAT (%)', true, true, NOW(), NOW()),
('business_inventory_hold_minutes', '15', 'number', 'BUSINESS', 'Thời gian giữ tồn kho khi checkout (phút)', true, true, NOW(), NOW()),
('business_order_auto_cancel_minutes', '30', 'number', 'BUSINESS', 'Tự động hủy đơn quá hạn thanh toán (phút)', true, true, NOW(), NOW()),
('business_return_period_days', '7', 'number', 'BUSINESS', 'Thời hạn đổi trả (ngày)', true, true, NOW(), NOW()),
('business_exchange_period_days', '7', 'number', 'BUSINESS', 'Thời hạn đổi hàng (ngày)', true, true, NOW(), NOW()),
('business_low_stock_threshold', '5', 'number', 'BUSINESS', 'Ngưỡng cảnh báo sắp hết hàng', true, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
config_key = VALUES(config_key);

-- 6. DEFAULT HOMEPAGE BANNERS
INSERT INTO banners (
id, title, image_url, link_url, display_order, position, is_active,
start_date, end_date, device_type, created_at, updated_at
) VALUES
(1, 'SOBU STUDIO', 'https://images.unsplash.com/photo-1614200187524-dc4b892acf16?q=80&w=2000&auto=format&fit=crop', '/products', 1, 'home_hero_carousel', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(2, 'HOT WHEELS', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/products', 2, 'home_hero_carousel', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(3, 'MECHA & GUNDAM', 'https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=800&auto=format&fit=crop', '/services', 3, 'home_hero_carousel', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(4, 'Sidebar left promotion', 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?q=80&w=800&auto=format&fit=crop', '/products', 1, 'site_left_sidebar_banner', true, '2026-01-01 00:00:00', NULL, 'WEB', NOW(), NOW()),
(5, 'Sidebar right promotion', 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?q=80&w=800&auto=format&fit=crop', '/products', 1, 'site_right_sidebar_banner', true, '2026-01-01 00:00:00', NULL, 'WEB', NOW(), NOW()),
(6, 'Bán chạy section banner', 'https://i0.wp.com/www.comicbookrevolution.com/wp-content/uploads/2023/12/transformers-4-previw-banner.jpg', '/products', 1, 'home_section_01_banner', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(7, 'Custom service primary', 'https://images.unsplash.com/photo-1730110206448-10297c1902bd?q=80&w=800&auto=format&fit=crop', '/services', 1, 'home_custom_service_image_primary', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(8, 'Custom service secondary', 'https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=800&auto=format&fit=crop', '/services', 1, 'home_custom_service_image_secondary', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(9, 'Custom service tertiary', 'https://images.unsplash.com/photo-1532581140115-3e355d1ed1de?q=80&w=600&auto=format&fit=crop', '/services', 1, 'home_custom_service_image_tertiary', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(10, 'Marvel category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/marvel', 1, 'home_category_card_01', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(11, 'DC category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/dc', 1, 'home_category_card_02', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(12, 'Hot Wheels category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/hot wheels', 1, 'home_category_card_03', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(13, 'Transformer category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/transformer', 1, 'home_category_card_04', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(14, 'Naruto category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/naruto', 1, 'home_category_card_05', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(15, 'Pacific Rim category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/pacific rim', 1, 'home_category_card_06', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(16, 'Dụng Cụ section banner', 'https://tooltechvietnam.com/wp-content/uploads/2023/03/handtools.jpg', '/products', 1, 'home_section_02_banner', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(17, 'Promo grid top left', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/products', 1, 'home_promo_grid_top_left', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(18, 'Promo grid bottom left', 'https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg', '/products', 1, 'home_promo_grid_bottom_left', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(19, 'Promo grid top right', 'https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg', '/products', 1, 'home_promo_grid_top_right', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(20, 'Promo grid bottom right', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/products', 1, 'home_promo_grid_bottom_right', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(21, 'Hotwheels section banner', 'https://images.unsplash.com/photo-1551522435-a13afa10f103?q=80&w=1600&auto=format&fit=crop', '/products', 1, 'home_section_03_banner', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW()),
(22, 'Sale section banner', 'https://img.magnific.com/free-vector/modern-black-friday-holiday-sale-offer-banner-get-30-percent-price-drop-vector_1017-47794.jpg?semt=ais_hybrid&w=740&q=80', '/products', 1, 'home_section_04_banner', true, '2026-01-01 00:00:00', NULL, 'ALL', NOW(), NOW())
ON DUPLICATE KEY UPDATE
title = VALUES(title),
image_url = VALUES(image_url),
link_url = VALUES(link_url),
display_order = VALUES(display_order),
position = VALUES(position),
is_active = VALUES(is_active),
device_type = VALUES(device_type);
