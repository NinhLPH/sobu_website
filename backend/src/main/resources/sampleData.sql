-- ========================================================
-- SOBU DEVELOPMENT & TEST SAMPLE DATA
-- Purpose: Complete seed data for local development & testing
-- Safe & Idempotent: Uses ON DUPLICATE KEY UPDATE / INSERT IGNORE
-- ========================================================

SET NAMES 'utf8mb4';
SET CHARACTER SET utf8mb4;

-- ========================
-- 1. ROLES
-- ========================
INSERT INTO role (name, description) VALUES 
('ADMIN', 'Quản trị viên hệ thống'),
('USER', 'Khách hàng mua sắm'),
('MANAGER', 'Quản lý'),
('STAFF', 'Nhân viên kho và đơn hàng')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ========================
-- 2. LOYALTY TIERS & RULES
-- ========================
INSERT INTO loyalty_tiers (id, name, min_total_money, discount_rate) VALUES
(1, 0, 0, 0),          -- Đồng
(2, 1, 5000000, 3),    -- Bạc
(3, 2, 15000000, 7)    -- Vàng
ON DUPLICATE KEY UPDATE
name = VALUES(name),
min_total_money = VALUES(min_total_money),
discount_rate = VALUES(discount_rate);

INSERT INTO loyalty_rules (id, code, `value`, active) VALUES
(1, 'EARN_RATE', '10000:1', true),
(2, 'REDEEM_RATE', '1:1000', true),
(3, 'BIRTHDAY_BONUS', '200', true)
ON DUPLICATE KEY UPDATE
code = VALUES(code),
`value` = VALUES(`value`),
active = VALUES(active);

-- ========================
-- 3. ACCOUNTS & CUSTOMERS
-- Default password for all sample accounts: "password"
-- ========================
INSERT INTO account (id, role_id, email, phone, password_hash, full_name, status) VALUES
(1, (SELECT id FROM role WHERE name = 'ADMIN'), 'admin@sobu.vn', '0901000001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Quản trị viên SOBU', 'ACTIVE'),
(2, (SELECT id FROM role WHERE name = 'MANAGER'), 'manager@sobu.vn', '0901000002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Nguyễn Minh Quản', 'ACTIVE'),
(3, (SELECT id FROM role WHERE name = 'STAFF'), 'staff@sobu.vn', '0901000003', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Trần Thu Kho', 'ACTIVE'),
(4, (SELECT id FROM role WHERE name = 'USER'), 'linh.nguyen@example.com', '0912000001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Nguyễn Hoàng Linh', 'ACTIVE'),
(5, (SELECT id FROM role WHERE name = 'USER'), 'minh.tran@example.com', '0912000002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Trần Gia Minh', 'ACTIVE'),
(6, (SELECT id FROM role WHERE name = 'USER'), 'ha.pham@example.com', '0912000003', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Phạm Ngọc Hà', 'INACTIVE')
ON DUPLICATE KEY UPDATE
role_id = VALUES(role_id),
phone = VALUES(phone),
password_hash = VALUES(password_hash),
full_name = VALUES(full_name),
status = VALUES(status);

INSERT INTO customers (id, account_id, gender, birthday, province, district, ward, street, total_money, points, tier_id) VALUES
(1, 4, 2, '1996-04-12', 'TP. Hồ Chí Minh', 'Quận 1', 'Phường Bến Nghé', '12 Lê Lợi', 6250000, 625, 2),
(2, 5, 1, '1992-09-21', 'Hà Nội', 'Quận Hoàn Kiếm', 'Phường Hàng Trống', '26 Tràng Thi', 18400000, 1840, 3),
(3, 6, 2, '2000-01-08', 'Đà Nẵng', 'Quận Hải Châu', 'Phường Hải Châu I', '08 Bạch Đằng', 450000, 45, 1)
ON DUPLICATE KEY UPDATE
gender = VALUES(gender),
birthday = VALUES(birthday),
province = VALUES(province),
district = VALUES(district),
ward = VALUES(ward),
street = VALUES(street),
total_money = VALUES(total_money),
points = VALUES(points),
tier_id = VALUES(tier_id);

INSERT INTO loyalty_transactions (id, customer_id, type, points, source, reference_id, note, created_at) VALUES
(1, 1, 'EARN', 125, 'ORDER', 1, 'Tích điểm từ đơn hàng SOBU-ORD-0001', '2026-05-18 09:30:00'),
(2, 2, 'EARN', 240, 'ORDER', 2, 'Tích điểm từ đơn hàng SOBU-ORD-0002', '2026-05-19 14:10:00'),
(3, 3, 'ADJUST', 45, 'PROMOTION', NULL, 'Tặng điểm chào mừng khách hàng mới', '2026-05-20 08:00:00')
ON DUPLICATE KEY UPDATE
customer_id = VALUES(customer_id),
type = VALUES(type),
points = VALUES(points),
source = VALUES(source),
reference_id = VALUES(reference_id),
note = VALUES(note),
created_at = VALUES(created_at);

-- ========================
-- 4. CATEGORIES & BRANDS
-- ========================
INSERT INTO categories (id, parent_id, code, name, slug, seo_title, meta_description, sort_order, image, image_alt, content, status) VALUES
(100, NULL, 'SKINCARE', 'Chăm sóc da', 'cham-soc-da', 'Sản Phẩm Chăm Sóc Da Chính Hãng | Sobu Store', 'Khám phá bộ sưu tập chăm sóc da mặt và toàn thân chính hãng tại Sobu.', 1, '/images/categories/skincare.jpg', 'Danh mục chăm sóc da', 'Sản phẩm chăm sóc da mặt và cơ thể.', 1),
(101, 100, 'CLEANSER', 'Sữa rửa mặt', 'sua-rua-mat', 'Sữa Rửa Mặt Dịu Nhẹ Cho Mọi Loại Da | Sobu Store', 'Sữa rửa mặt làm sạch sâu, không khô căng, an toàn cho da nhạy cảm.', 1, '/images/categories/cleanser.jpg', 'Danh mục sữa rửa mặt', 'Làm sạch dịu nhẹ hằng ngày.', 1),
(102, 100, 'SUNSCREEN', 'Kem chống nắng', 'kem-chong-nang', 'Kem Chống Nắng Phổ Rộng SPF50+ | Sobu Store', 'Kem chống nắng bảo vệ da toàn diện trước tia UVA, UVB.', 2, '/images/categories/sunscreen.jpg', 'Danh mục kem chống nắng', 'Bảo vệ da trước tia UV.', 1),
(200, NULL, 'MAKEUP', 'Trang điểm', 'trang-diem', 'Mỹ Phẩm Trang Điểm Cao Cấp | Sobu Store', 'Bộ sưu tập đồ trang điểm cá nhân xu hướng mới nhất.', 2, '/images/categories/makeup.jpg', 'Danh mục trang điểm', 'Sản phẩm trang điểm cá nhân.', 1),
(201, 200, 'LIPSTICK', 'Son môi', 'son-moi', 'Son Môi Cao Cấp Lên Màu Chuẩn | Sobu Store', 'Son lì, son bóng, son dưỡng chính hãng bền màu lâu trôi.', 1, '/images/categories/lipstick.jpg', 'Danh mục son môi', 'Son môi nhiều tông màu.', 1)
ON DUPLICATE KEY UPDATE
parent_id = VALUES(parent_id),
code = VALUES(code),
name = VALUES(name),
slug = VALUES(slug),
seo_title = VALUES(seo_title),
meta_description = VALUES(meta_description),
sort_order = VALUES(sort_order),
image = VALUES(image),
image_alt = VALUES(image_alt),
content = VALUES(content),
status = VALUES(status);

INSERT INTO brands (id, code, name, slug, seo_title, meta_description, status, parent_id, created_at) VALUES
(10, 'SODU', 'Sodu Beauty', 'sodu-beauty', 'Mỹ Phẩm Sodu Beauty Chính Hãng | Sobu Store', 'Thương hiệu mỹ phẩm thiên nhiên Sodu Beauty cao cấp.', 1, NULL, '2026-05-01 08:00:00'),
(11, 'SODU-LAB', 'Sodu Lab', 'sodu-lab', 'Dược Mỹ Phẩm Sodu Lab | Sobu Store', 'Dòng dược mỹ phẩm phục hồi chuyên sâu Sodu Lab.', 1, 10, '2026-05-01 08:05:00'),
(20, 'AURORA', 'Aurora Skincare', 'aurora-skincare', 'Aurora Skincare Hàn Quốc | Sobu Store', 'Dưỡng da chuyên sâu từ Aurora Skincare.', 1, NULL, '2026-05-02 09:00:00'),
(30, 'MELIA', 'Melia Cosmetics', 'melia-cosmetics', 'Melia Cosmetics Ý | Sobu Store', 'Trang điểm phong cách châu Âu cùng Melia Cosmetics.', 1, NULL, '2026-05-03 10:00:00')
ON DUPLICATE KEY UPDATE
code = VALUES(code),
name = VALUES(name),
slug = VALUES(slug),
seo_title = VALUES(seo_title),
meta_description = VALUES(meta_description),
status = VALUES(status),
parent_id = VALUES(parent_id),
created_at = VALUES(created_at);

-- ========================
-- 5. PRODUCTS & DETAILS
-- ========================
INSERT INTO products (
id, external_id, parent_id, code, barcode, name, slug, seo_title, meta_description, h1_title, other_name, status,
category_id, category_name, internal_category_id, internal_category_name,
brand_id, brand_name, type_id, type_name, supplier_id, supplier_name, supplier_phone,
retail_price, import_price, wholesale_price, old_price, sale_price, currency, condition_type, availability, avg_cost, vat,
avatar_image, length, width, height, weight, country_name,
stock_remain, stock_available, description, content, created_at, updated_at, raw_data, active
) VALUES
(1001, 9001001, NULL, 'SD-CLEANSER-120', '8938500000011', 'Sữa rửa mặt Sodu Gentle 120ml', 'sua-rua-mat-sodu-gentle-120ml', 'Sữa Rửa Mặt Sodu Gentle 120ml Dịu Nhẹ | Sobu Store', 'Sữa rửa mặt Sodu Gentle 120ml dịu nhẹ làm sạch sâu, giữ ẩm, an toàn cho da nhạy cảm.', 'Sữa Rửa Mặt Sodu Gentle 120ml Chính Hãng', 'Gentle Cleanser', 'ACTIVE',
101, 'Sữa rửa mặt', 101, 'Sữa rửa mặt',
10, 'Sodu Beauty', 1, 'Sản phẩm thường', 501, 'Sodu Distribution', '02873000001',
189000, 98000, 155000, 229000, 189000, 'VND', 'NEW', 'IN_STOCK', 102000, 8,
'/images/products/sd-cleanser-120.jpg', 12, 5, 5, 180, 'Việt Nam',
120, 110, 'Sữa rửa mặt dịu nhẹ cho da thường và da nhạy cảm.', 'Làm sạch bụi bẩn, dầu thừa mà không gây khô căng.', '2026-05-10 09:00:00', '2026-05-20 11:00:00', '{"source":"sample"}', TRUE),
(1002, 9001002, NULL, 'AR-SUNSCREEN-50', '8938500000028', 'Kem chống nắng Aurora SPF50 PA++++ 50ml', 'kem-chong-nang-aurora-spf50-pa-50ml', 'Kem Chống Nắng Aurora SPF50+ PA++++ 50ml | Sobu Store', 'Kem chống nắng Aurora bảo vệ da toàn diện, kiềm dầu, không nhờn rít suốt ngày dài.', 'Kem Chống Nắng Aurora SPF50+ PA++++ 50ml', 'Aurora Sunscreen', 'ACTIVE',
102, 'Kem chống nắng', 102, 'Kem chống nắng',
20, 'Aurora Skincare', 1, 'Sản phẩm thường', 502, 'Aurora Việt Nam', '02873000002',
329000, 190000, 285000, 389000, 329000, 'VND', 'NEW', 'IN_STOCK', 198000, 8,
'/images/products/ar-sunscreen-50.jpg', 14, 4, 4, 120, 'Hàn Quốc',
75, 70, 'Kem chống nắng phổ rộng, kết cấu mỏng nhẹ.', 'Phù hợp dùng hằng ngày dưới lớp trang điểm.', '2026-05-11 10:00:00', '2026-05-20 11:05:00', '{"source":"sample"}', TRUE),
(1003, 9001003, NULL, 'ML-LIP-M01', '8938500000035', 'Son lì Melia Velvet màu Rose Mood', 'son-li-melia-velvet-mau-rose-mood', 'Son Lì Melia Velvet Màu Rose Mood | Sobu Store', 'Son lì Melia Velvet màu Rose Mood quyến rũ, mềm môi, bền màu đến 8 tiếng.', 'Son Lì Melia Velvet Màu Rose Mood Chính Hãng', 'Melia Velvet Rose Mood', 'ACTIVE',
201, 'Son môi', 201, 'Son môi',
30, 'Melia Cosmetics', 1, 'Sản phẩm thường', 503, 'Melia Official', '02873000003',
249000, 120000, 210000, 299000, 249000, 'VND', 'NEW', 'IN_STOCK', 128000, 8,
'/images/products/ml-lip-m01.jpg', 9, 2, 2, 60, 'Ý',
210, 205, 'Son lì mềm môi, màu rose mood dễ dùng.', 'Chất son mịn, bám màu tốt trong nhiều giờ.', '2026-05-12 13:30:00', '2026-05-20 11:10:00', '{"source":"sample"}', TRUE)
ON DUPLICATE KEY UPDATE
external_id = VALUES(external_id),
code = VALUES(code),
barcode = VALUES(barcode),
name = VALUES(name),
slug = VALUES(slug),
seo_title = VALUES(seo_title),
meta_description = VALUES(meta_description),
h1_title = VALUES(h1_title),
other_name = VALUES(other_name),
status = VALUES(status),
category_id = VALUES(category_id),
category_name = VALUES(category_name),
internal_category_id = VALUES(internal_category_id),
internal_category_name = VALUES(internal_category_name),
brand_id = VALUES(brand_id),
brand_name = VALUES(brand_name),
retail_price = VALUES(retail_price),
import_price = VALUES(import_price),
wholesale_price = VALUES(wholesale_price),
old_price = VALUES(old_price),
sale_price = VALUES(sale_price),
currency = VALUES(currency),
condition_type = VALUES(condition_type),
availability = VALUES(availability),
avg_cost = VALUES(avg_cost),
vat = VALUES(vat),
avatar_image = VALUES(avatar_image),
stock_remain = VALUES(stock_remain),
stock_available = VALUES(stock_available),
description = VALUES(description),
content = VALUES(content),
updated_at = VALUES(updated_at),
raw_data = VALUES(raw_data),
active = VALUES(active);

INSERT INTO product_units (id, product_id, name, quantity, price, wholesale_price) VALUES
(1, 1001, 'Chai', 1, 189000, 155000),
(2, 1001, 'Combo 2 chai', 2, 360000, 300000),
(3, 1002, 'Tuýp', 1, 329000, 285000),
(4, 1003, 'Thỏi', 1, 249000, 210000)
ON DUPLICATE KEY UPDATE
product_id = VALUES(product_id),
name = VALUES(name),
quantity = VALUES(quantity),
price = VALUES(price),
wholesale_price = VALUES(wholesale_price);

INSERT INTO product_attributes (id, product_id, name, value) VALUES
(1, 1001, 'Dung tích', '120ml'),
(2, 1001, 'Loại da', 'Da thường, da nhạy cảm'),
(3, 1002, 'Chỉ số chống nắng', 'SPF50 PA++++'),
(4, 1003, 'Màu sắc', 'Rose Mood')
ON DUPLICATE KEY UPDATE
product_id = VALUES(product_id),
name = VALUES(name),
value = VALUES(value);

INSERT INTO product_images (id, product_id, url, alt_text, sort_order, is_avatar) VALUES
(1, 1001, '/images/products/sd-cleanser-120-1.jpg', 'Sữa rửa mặt Sodu Gentle 120ml mặt trước', 1, true),
(2, 1001, '/images/products/sd-cleanser-120-2.jpg', 'Sữa rửa mặt Sodu Gentle 120ml mặt sau', 2, false),
(3, 1002, '/images/products/ar-sunscreen-50-1.jpg', 'Kem chống nắng Aurora SPF50 PA++++ 50ml', 1, true),
(4, 1003, '/images/products/ml-lip-m01-1.jpg', 'Son lì Melia Velvet màu Rose Mood', 1, true)
ON DUPLICATE KEY UPDATE
product_id = VALUES(product_id),
url = VALUES(url),
alt_text = VALUES(alt_text),
sort_order = VALUES(sort_order),
is_avatar = VALUES(is_avatar);

INSERT INTO product_videos (id, product_id, title, src) VALUES
(1, 1001, 'Hướng dẫn dùng Sodu Gentle Cleanser', '/videos/products/sd-cleanser-demo.mp4'),
(2, 1002, 'Test kết cấu Aurora Sunscreen', '/videos/products/ar-sunscreen-texture.mp4')
ON DUPLICATE KEY UPDATE
product_id = VALUES(product_id),
title = VALUES(title),
src = VALUES(src);

-- ========================
-- 6. REQUESTS & DETAILS
-- ========================
INSERT INTO requests (
id, request_code, customer_phone, version, status, type, total_amount, deposit_amount,
custom_requirements, nhanh_order_id, nhanh_order_code, admin_id, created_at, updated_at
) VALUES
(1, 'SOBU-REQ-0001', '0912000001', 0, 'APPROVED', 'NORMAL', 738000, 0, '{"note":"Giao giờ hành chính","preferredChannel":"phone"}', 'NH-10001', 'NH-SODU-10001', 2, '2026-05-18 09:00:00', '2026-05-18 09:20:00'),
(2, 'SOBU-REQ-0002', '0912000002', 0, 'APPROVED', 'PREORDER', 498000, 100000, '{"note":"Đặt trước 2 thỏi son","giftWrap":true}', NULL, NULL, 2, '2026-05-19 13:40:00', '2026-05-20 09:05:00'),
(3, 'SOBU-REQ-0003', '0912000003', 0, 'REVIEWING', 'FINDING', 0, 0, '{"lookingFor":"Serum phục hồi cho da nhạy cảm","budget":"500000"}', NULL, NULL, 3, '2026-05-20 08:30:00', '2026-05-20 09:00:00')
ON DUPLICATE KEY UPDATE
customer_phone = VALUES(customer_phone),
version = VALUES(version),
status = VALUES(status),
type = VALUES(type),
total_amount = VALUES(total_amount),
deposit_amount = VALUES(deposit_amount),
custom_requirements = VALUES(custom_requirements),
nhanh_order_id = VALUES(nhanh_order_id),
nhanh_order_code = VALUES(nhanh_order_code),
admin_id = VALUES(admin_id),
updated_at = VALUES(updated_at);

INSERT INTO request_items (id, request_id, nhanh_product_id, name, note, metadata_json, price, quantity) VALUES
(1, 1, '9001001', 'Sữa rửa mặt Sodu Gentle 120ml', 'Khách chọn combo 2 chai', '{"productId":1001,"unit":"Combo 2 chai"}', 360000, 1),
(2, 1, '9001002', 'Kem chống nắng Aurora SPF50 PA++++ 50ml', NULL, '{"productId":1002,"unit":"Tuýp"}', 329000, 1),
(3, 2, '9001003', 'Son lì Melia Velvet màu Rose Mood', 'Gói quà', '{"productId":1003,"color":"Rose Mood"}', 249000, 2),
(4, 3, NULL, 'Serum phục hồi da nhạy cảm', 'Tìm sản phẩm không hương liệu', '{"skinType":"sensitive","maxPrice":500000}', 0, 1)
ON DUPLICATE KEY UPDATE
request_id = VALUES(request_id),
nhanh_product_id = VALUES(nhanh_product_id),
name = VALUES(name),
note = VALUES(note),
metadata_json = VALUES(metadata_json),
price = VALUES(price),
quantity = VALUES(quantity);

INSERT INTO request_attachments (id, request_id, url, type, mime_type, size, sort_order, uploaded_by, created_at) VALUES
(1, 3, '/uploads/requests/skin-reference-01.jpg', 'IMAGE', 'image/jpeg', 245760, 1, 'ha.pham@example.com', '2026-05-20 08:35:00'),
(2, 3, '/uploads/requests/product-reference-01.png', 'IMAGE', 'image/png', 312400, 2, 'ha.pham@example.com', '2026-05-20 08:36:00')
ON DUPLICATE KEY UPDATE
request_id = VALUES(request_id),
url = VALUES(url),
type = VALUES(type),
mime_type = VALUES(mime_type),
size = VALUES(size),
sort_order = VALUES(sort_order),
uploaded_by = VALUES(uploaded_by),
created_at = VALUES(created_at);

INSERT INTO request_timelines (id, request_id, action, from_status, to_status, actor, note, created_at) VALUES
(1, 1, 'CREATE_REQUEST', NULL, 'PENDING', 'linh.nguyen@example.com', 'Khách tạo yêu cầu mua hàng.', '2026-05-18 09:00:00'),
(2, 1, 'APPROVE_REQUEST', 'REVIEWING', 'APPROVED', 'manager@sobu.vn', 'Đã xác nhận tồn kho và duyệt yêu cầu.', '2026-05-18 09:20:00'),
(3, 3, 'START_REVIEW', 'PENDING', 'REVIEWING', 'staff@sobu.vn', 'Nhân viên bắt đầu tìm sản phẩm phù hợp.', '2026-05-20 09:00:00')
ON DUPLICATE KEY UPDATE
request_id = VALUES(request_id),
action = VALUES(action),
from_status = VALUES(from_status),
to_status = VALUES(to_status),
actor = VALUES(actor),
note = VALUES(note),
created_at = VALUES(created_at);

INSERT INTO request_snapshots (id, request_id, snapshot_type, snapshot_json, captured_at) VALUES
(1, 1, 'APPROVED_REQUEST', '{"requestCode":"SOBU-REQ-0001","totalAmount":738000,"items":2}', '2026-05-18 09:20:00'),
(2, 2, 'APPROVED_REQUEST', '{"requestCode":"SOBU-REQ-0002","totalAmount":498000,"items":1}', '2026-05-19 14:00:00'),
(3, 3, 'INTAKE', '{"requestCode":"SOBU-REQ-0003","need":"Serum phục hồi da nhạy cảm"}', '2026-05-20 08:35:00')
ON DUPLICATE KEY UPDATE
request_id = VALUES(request_id),
snapshot_type = VALUES(snapshot_type),
snapshot_json = VALUES(snapshot_json),
captured_at = VALUES(captured_at);

-- ========================
-- 7. ORDERS & PAYMENTS
-- ========================
INSERT INTO orders (
id, order_code, app_order_id, request_id, type, status, sync_status, nhanh_sync_stage, total_amount, deposit_amount, shipping_fee,
paid_amount, remaining_amount, payment_status, description, customer_name, customer_mobile, customer_email, customer_address, customer_city_name,
customer_district_name, customer_ward_name, customer_city_id, customer_district_id, customer_ward_id, carrier_id, carrier_service_id,
location_version, nhanh_order_id, nhanh_order_code, sync_error, last_sync_message, last_sync_at, version, created_at, updated_at
) VALUES
(1, 'SOBU-ORD-0001', 'SOBU-ORD-0001', 1, 'NORMAL', 'PROCESSING', 'SYNCED', 'NORMAL_ORDER_CREATED', 738000, 0, 0,
738000, 0, 'PAID', 'Đơn hàng từ yêu cầu SOBU-REQ-0001.',
'Nguyễn Hoàng Linh', '0912000001', 'linh.nguyen@example.com', '12 Lê Lợi', 'TP. Hồ Chí Minh', 'Quận 1', 'Phường Bến Nghé',
79, 760, 26734, 8, 1, 'v1', 'NH-10001', 'NH-SODU-10001', NULL, 'Nhanh normal order created successfully.', '2026-05-18 09:45:00',
0, '2026-05-18 09:30:00', '2026-05-18 09:45:00'),
(2, 'SOBU-ORD-0002', 'SOBU-ORD-0002', 2, 'PREORDER', 'READY_FOR_FINAL_PAYMENT', 'SYNCED', 'PREORDER_DEPOSIT_CREATED', 498000, 100000, 0,
100000, 398000, 'PENDING', 'Đơn đặt trước từ yêu cầu SOBU-REQ-0002.',
'Trần Gia Minh', '0912000002', 'minh.tran@example.com', '26 Tràng Thi', 'Hà Nội', 'Quận Hoàn Kiếm', 'Phường Hàng Trống',
1, 1, 1, 8, 1, 'v1', 'NH-10002', 'NH-SODU-10002', NULL, 'Nhanh preorder deposit order created successfully.', '2026-05-19 14:30:00',
0, '2026-05-19 14:10:00', '2026-05-20 09:10:00'),
(3, 'SOBU-ORD-0003', 'SOBU-ORD-0003', NULL, 'NORMAL', 'DELIVERED', 'SYNCED', 'NORMAL_ORDER_CREATED', 518000, 0, 35000,
518000, 0, 'PAID', 'Đơn hàng mẫu đã giao thành công — dùng để test review.',
'Nguyễn Hoàng Linh', '0912000001', 'linh.nguyen@example.com', '12 Lê Lợi', 'TP. Hồ Chí Minh', 'Quận 1', 'Phường Bến Nghé',
79, 760, 26734, 8, 1, 'v1', NULL, NULL, NULL, NULL, NULL,
0, '2026-06-15 09:00:00', '2026-06-20 15:30:00')
ON DUPLICATE KEY UPDATE
app_order_id = VALUES(app_order_id),
request_id = VALUES(request_id),
type = VALUES(type),
status = VALUES(status),
sync_status = VALUES(sync_status),
nhanh_sync_stage = VALUES(nhanh_sync_stage),
total_amount = VALUES(total_amount),
deposit_amount = VALUES(deposit_amount),
shipping_fee = VALUES(shipping_fee),
paid_amount = VALUES(paid_amount),
remaining_amount = VALUES(remaining_amount),
payment_status = VALUES(payment_status),
description = VALUES(description),
customer_name = VALUES(customer_name),
customer_mobile = VALUES(customer_mobile),
customer_email = VALUES(customer_email),
customer_address = VALUES(customer_address),
customer_city_name = VALUES(customer_city_name),
customer_district_name = VALUES(customer_district_name),
customer_ward_name = VALUES(customer_ward_name),
customer_city_id = VALUES(customer_city_id),
customer_district_id = VALUES(customer_district_id),
customer_ward_id = VALUES(customer_ward_id),
carrier_id = VALUES(carrier_id),
carrier_service_id = VALUES(carrier_service_id),
location_version = VALUES(location_version),
nhanh_order_id = VALUES(nhanh_order_id),
nhanh_order_code = VALUES(nhanh_order_code),
sync_error = VALUES(sync_error),
last_sync_message = VALUES(last_sync_message),
last_sync_at = VALUES(last_sync_at),
updated_at = VALUES(updated_at);

INSERT INTO order_items (id, order_id, nhanh_product_id, name, note, price, discount, quantity) VALUES
(1, 1, '9001001', 'Sữa rửa mặt Sodu Gentle 120ml', 'Combo 2 chai', 360000, 0, 1),
(2, 1, '9001002', 'Kem chống nắng Aurora SPF50 PA++++ 50ml', NULL, 329000, 0, 1),
(3, 2, '9001003', 'Son lì Melia Velvet màu Rose Mood', 'Gói quà', 249000, 0, 2),
(4, 3, '9001001', 'Sữa rửa mặt Sodu Gentle 120ml', 'Đơn hàng giao thành công', 189000, 0, 1),
(5, 3, '9001002', 'Kem chống nắng Aurora SPF50 PA++++ 50ml', 'Đơn hàng giao thành công', 329000, 0, 1)
ON DUPLICATE KEY UPDATE
order_id = VALUES(order_id),
nhanh_product_id = VALUES(nhanh_product_id),
name = VALUES(name),
note = VALUES(note),
price = VALUES(price),
discount = VALUES(discount),
quantity = VALUES(quantity);

INSERT INTO order_payments (
id, order_id, payment_code, type, payment_method, status, amount, provider, provider_reference,
checkout_url, qr_code, failure_reason, expires_at, paid_at, version, created_at, updated_at
) VALUES
(1, 1, 'SOBU-PAY-0001', 'FULL', 'ONLINE', 'PAID', 738000, 'PAYOS_MOCK', 'PAYOS-MOCK-0001',
'https://pay.payos.vn/mock/SOBU-PAY-0001', 'qr://SOBU-PAY-0001', NULL, '2026-05-18 10:30:00', '2026-05-18 09:35:00', 0, '2026-05-18 09:30:00', '2026-05-18 09:35:00'),
(2, 2, 'SOBU-PAY-0002', 'DEPOSIT', 'ONLINE', 'PAID', 100000, 'PAYOS_MOCK', 'PAYOS-MOCK-0002',
'https://pay.payos.vn/mock/SOBU-PAY-0002', 'qr://SOBU-PAY-0002', NULL, '2026-05-19 18:00:00', '2026-05-19 14:30:00', 0, '2026-05-19 14:10:00', '2026-05-19 14:30:00'),
(3, 2, 'SOBU-PAY-0003', 'FINAL', 'ONLINE', 'PENDING', 398000, 'PAYOS_MOCK', 'PAYOS-MOCK-0003',
'https://pay.payos.vn/mock/SOBU-PAY-0003', 'qr://SOBU-PAY-0003', NULL, '2026-05-21 18:00:00', NULL, 0, '2026-05-20 09:10:00', '2026-05-20 09:10:00')
ON DUPLICATE KEY UPDATE
order_id = VALUES(order_id),
payment_code = VALUES(payment_code),
type = VALUES(type),
payment_method = VALUES(payment_method),
status = VALUES(status),
amount = VALUES(amount),
provider = VALUES(provider),
provider_reference = VALUES(provider_reference),
checkout_url = VALUES(checkout_url),
qr_code = VALUES(qr_code),
failure_reason = VALUES(failure_reason),
expires_at = VALUES(expires_at),
paid_at = VALUES(paid_at),
updated_at = VALUES(updated_at);

-- ========================
-- 8. STATIC PAGES
-- ========================
INSERT INTO static_pages (
id, slug, title, html_content, is_published, created_at, updated_at
) VALUES
(1, 'about', 'Giới thiệu', '<h1>Về SOBU Studio</h1><p>SOBU Studio chuyên cung cấp mô hình sưu tầm chính hãng, dịch vụ đặt trước (pre-order), tìm kiếm mô hình hiếm và custom theo yêu cầu.</p>', true, NOW(), NOW()),
(2, 'privacy-policy', 'Chính sách bảo mật', '<h1>Chính sách bảo mật</h1><p>SOBU cam kết bảo mật thông tin cá nhân của khách hàng và chỉ sử dụng cho mục đích xử lý đơn hàng và hỗ trợ khách hàng.</p>', true, NOW(), NOW()),
(3, 'terms', 'Điều khoản dịch vụ', '<h1>Điều khoản dịch vụ</h1><p>Bằng việc sử dụng website và dịch vụ của SOBU Studio, quý khách đồng ý tuân thủ các quy định đặt hàng, thanh toán và vận chuyển được công bố công khai.</p>', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
title = VALUES(title),
html_content = VALUES(html_content),
is_published = VALUES(is_published),
updated_at = NOW();

-- ========================
-- 9. WEBSITE CONFIGURATIONS
-- ========================
INSERT INTO website_configurations (
config_key, config_value, type, group_name, description, is_public, is_active, created_at, updated_at
) VALUES
('primary_color', '#00618e', 'color', 'THEME', NULL, true, true, NOW(), NOW()),
('secondary_color', '#005f9c', 'color', 'THEME', NULL, true, true, NOW(), NOW()),
('accent_color', '#5a4bb4', 'color', 'THEME', NULL, true, true, NOW(), NOW()),
('background_color', '#f3f6ff', 'color', 'THEME', NULL, true, true, NOW(), NOW()),
('surface_color', '#ffffff', 'color', 'THEME', NULL, true, true, NOW(), NOW()),
('website_logo', 'https://placehold.co/240x80?text=SOBU', 'image', 'THEME', NULL, true, true, NOW(), NOW()),
('website_favicon', '/assets/favicon.png', 'image', 'THEME', NULL, true, true, NOW(), NOW()),
('product_placeholder_image', 'https://placehold.co/400x300?text=SOBU', 'image', 'THEME', NULL, true, true, NOW(), NOW()),
('banner_placeholder_image', 'https://placehold.co/1200x420?text=SOBU', 'image', 'THEME', NULL, true, true, NOW(), NOW()),
('seo_default_title', 'SOBU Studio - Mô hình sưu tầm & dịch vụ collector', 'text', 'SEO', NULL, true, true, NOW(), NOW()),
('seo_default_description', 'SOBU Studio cung cấp mô hình sưu tầm, dịch vụ đặt trước, tìm hàng và custom dành cho collector.', 'text', 'SEO', NULL, true, true, NOW(), NOW()),
('seo_default_keywords', 'sobu, sobu studio, mô hình sưu tầm, collector, pre-order, custom model', 'text', 'SEO', NULL, true, true, NOW(), NOW()),
('seo_og_title', 'SOBU Studio', 'text', 'SEO', NULL, true, true, NOW(), NOW()),
('seo_og_description', 'Khám phá mô hình sưu tầm, dịch vụ tìm hàng và đặt trước cùng SOBU.', 'text', 'SEO', NULL, true, true, NOW(), NOW()),
('seo_og_image', 'https://placehold.co/1200x630?text=SOBU+Studio', 'image', 'SEO', NULL, true, true, NOW(), NOW()),
('seo_robots_index_enabled', 'true', 'boolean_type', 'SEO', NULL, true, true, NOW(), NOW()),
('site_name', 'SOBU', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('store_display_name', 'SOBU Studio', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('support_hotline', '1900 636 999', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('support_email', 'support@sobu.vn', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('company_name', 'SOBU Studio', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('company_address', 'Hà Nam, Việt Nam', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('working_hours', '09:00 - 21:00, T2 - CN', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('footer_greeting_text', 'SOBU đồng hành cùng cộng đồng collector trong từng đơn hàng và yêu cầu đặc biệt.', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('copyright_text', '© 2026 SOBU Studio. All rights reserved.', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('newsletter_enabled', 'false', 'boolean_type', 'GENERAL', NULL, true, true, NOW(), NOW()),
('newsletter_description', 'Nhận thông tin về sản phẩm mới, hàng sắp về và ưu đãi riêng cho collector.', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('newsletter_submit_label', 'Đăng ký', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('footer_company_links', '[{"label":"Giới thiệu","href":"/about"},{"label":"Dịch vụ","href":"/services"},{"label":"Tin tức","href":"/blog"}]', 'json', 'FOOTER', NULL, true, true, NOW(), NOW()),
('footer_help_links', '[{"label":"Sản phẩm","href":"/products"},{"label":"Yêu cầu tìm hàng","href":"/request"},{"label":"Liên hệ","href":"/contact"}]', 'json', 'FOOTER', NULL, true, true, NOW(), NOW()),
('legal_links', '[{"label":"Điều khoản","href":"/terms"},{"label":"Bảo mật","href":"/privacy"}]', 'json', 'FOOTER', NULL, true, true, NOW(), NOW()),
('home_section_01_title', 'BÁN CHẠY', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_01_subtitle', 'Giao Hàng Toàn Quốc', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_01_label', 'Xem thêm', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_01_cta_url', '/products', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_custom_service_title', 'DỊCH VỤ ĐỘ MÔ HÌNH SỐ 1 VIỆT NAM', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_custom_service_badges', '["Độ Led cảm ứng","Sơn mô hình chuẩn phim","Custom theo ý thích"]', 'json', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_custom_service_cta_label', 'CUSTOM NGAY', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_custom_service_cta_url', '/services', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_02_title', 'MÔ HÌNH CUSTOM', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_02_subtitle', 'Giao Hàng Toàn Quốc', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_02_cta_label', 'Xem thêm', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_02_cta_url', '/products', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_category_title', 'Thể loại mô hình', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_category_cards', '[{"label":"Marvel","href":"/category/marvel","bannerPosition":"home_category_card_01"},{"label":"DC","href":"/category/dc","bannerPosition":"home_category_card_02"},{"label":"Hot Wheels","href":"/category/hot wheels","bannerPosition":"home_category_card_03"},{"label":"Transformer","href":"/category/transformer","bannerPosition":"home_category_card_04"},{"label":"Naruto","href":"/category/naruto","bannerPosition":"home_category_card_05"},{"label":"Pacific Rim","href":"/category/pacific rim","bannerPosition":"home_category_card_06"}]', 'json', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_03_title', 'Dụng Cụ', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_03_subtitle', 'Giao Hàng Toàn Quốc', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_03_cta_label', 'Xem thêm', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_03_cta_url', '/products', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_04_title', 'Hot Wheels', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_04_subtitle', 'Giao Hàng Toàn Quốc', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_04_cta_label', 'Xem thêm', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_04_cta_url', '/products', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_05_title', 'Giảm giá cực mạnh', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_05_subtitle', 'Ưu Đãi Có Hạn', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_05_cta_label', 'Xem thêm', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_05_cta_url', '/products', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_news_title', 'Tin Tức', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_news_more_label', 'XEM TẤT CẢ', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_news_more_url', '/blog', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_testimonials_title', 'Đánh giá từ khách hàng', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_left_title', 'HOT WHEELS', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_left_description', 'KHÁM PHÁ NHỮNG MẪU XE MÔ HÌNH HOT NHẤT DÀNH CHO NGƯỜI ĐAM MÊ TỐC ĐỘ.', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_left_cta_label', 'Xem thêm', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_left_cta_url', '/products', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_title', 'Sưu tầm huyền thoại', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_description', 'SƯU TẦM NHỮNG MẪU XE HUYỀN THOẠI - TỪ SIÊU XE HIỆN ĐẠI ĐẾN CLASSIC CỔ ĐIỂN.', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_cta_label', '', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_cta_url', '/products', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_right_title', 'Limited Edition Cars', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_right_description', 'DISCOVER LIMITED EDITION CARS AND EXCLUSIVE RELEASES FOR TRUE COLLECTORS.', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_right_cta_label', '', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_right_cta_url', '/products', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_title', 'GIFT FOR COLLECTORS', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_description', 'MÓN QUÀ HOÀN HẢO CHO NGƯỜI YÊU XE VÀ ĐAM MÊ MÔ HÌNH.', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_cta_label', 'Xem thêm', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_cta_url', '/products', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_partners_title', 'Đối tác chiến lược & Thương hiệu đồng hành', 'text', 'HOME_PARTNER', NULL, true, true, NOW(), NOW()),
('home_partner_brands', '[{"name":"BANDAI","logoUrl":"https://placehold.co/180x60/e60012/ffffff?text=BANDAI"},{"name":"HOT TOYS","logoUrl":"https://placehold.co/180x60/111111/f1b82d?text=HOT+TOYS"},{"name":"TAMIYA","logoUrl":"https://placehold.co/180x60/0054a6/ffffff?text=TAMIYA"},{"name":"LEGO","logoUrl":"https://placehold.co/180x60/ffd500/000000?text=LEGO"},{"name":"MATTEL","logoUrl":"https://placehold.co/180x60/e5142a/ffffff?text=MATTEL"},{"name":"HASBRO","logoUrl":"https://placehold.co/180x60/0072ce/ffffff?text=HASBRO"}]', 'json', 'HOME_PARTNER', NULL, true, true, NOW(), NOW()),
('maintenance_mode_enabled', 'false', 'boolean_type', 'GENERAL', NULL, true, true, NOW(), NOW()),
('maintenance_message', 'Website đang được bảo trì. Vui lòng quay lại sau.', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('social_links', '{"facebook":"https://facebook.com","instagram":"","tiktok":"","youtube":"","zalo":""}', 'json', 'SOCIAL', NULL, true, true, NOW(), NOW()),
('social_share_enabled', 'true', 'boolean_type', 'SOCIAL', NULL, true, true, NOW(), NOW()),
('social_chat_widget_enabled', 'false', 'boolean_type', 'SOCIAL', NULL, true, true, NOW(), NOW()),
('social_chat_config', '{"provider":"zalo","pageId":"","greetingText":"SOBU có thể hỗ trợ gì cho bạn?"}', 'json', 'SOCIAL', NULL, true, true, NOW(), NOW()),
('free_shipping_threshold', '500000', 'number', 'CHECKOUT', NULL, true, true, NOW(), NOW()),
('max_cart_items', '99', 'number', 'CHECKOUT', NULL, true, true, NOW(), NOW()),
('checkout_guest_checkout_enabled', 'false', 'boolean_type', 'CHECKOUT', NULL, true, true, NOW(), NOW()),
('checkout_cod_enabled', 'true', 'boolean_type', 'CHECKOUT', NULL, true, true, NOW(), NOW()),
('checkout_online_payment_enabled', 'true', 'boolean_type', 'CHECKOUT', NULL, true, true, NOW(), NOW()),
('checkout_default_payment_method', 'ONLINE', 'text', 'CHECKOUT', NULL, true, true, NOW(), NOW()),
('checkout_shipping_fee_default', '0', 'number', 'CHECKOUT', NULL, true, true, NOW(), NOW()),
('checkout_order_note_enabled', 'true', 'boolean_type', 'CHECKOUT', NULL, true, true, NOW(), NOW()),
('business_currency', 'VND', 'text', 'BUSINESS', NULL, true, true, NOW(), NOW()),
('business_vat_rate', '10', 'number', 'BUSINESS', NULL, true, true, NOW(), NOW()),
('business_inventory_hold_minutes', '15', 'number', 'BUSINESS', NULL, true, true, NOW(), NOW()),
('business_order_auto_cancel_minutes', '30', 'number', 'BUSINESS', NULL, true, true, NOW(), NOW()),
('business_return_period_days', '7', 'number', 'BUSINESS', NULL, true, true, NOW(), NOW()),
('business_exchange_period_days', '7', 'number', 'BUSINESS', NULL, true, true, NOW(), NOW()),
('business_low_stock_threshold', '5', 'number', 'BUSINESS', NULL, true, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
config_key = VALUES(config_key);

-- ========================
-- 10. BANNERS
-- ========================
INSERT INTO banners (
id, title, image_url, link_url, display_order, position, is_active,
start_date, end_date, device_type, created_at, updated_at
) VALUES
(1, 'SOBU STUDIO', 'https://images.unsplash.com/photo-1614200187524-dc4b892acf16?q=80&w=2000&auto=format&fit=crop', '/products', 1, 'home_hero_carousel', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 08:00:00', '2026-05-01 08:00:00'),
(2, 'HOT WHEELS', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/products', 2, 'home_hero_carousel', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 08:10:00', '2026-05-01 08:10:00'),
(3, 'MECHA & GUNDAM', 'https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=800&auto=format&fit=crop', '/services', 3, 'home_hero_carousel', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 08:20:00', '2026-05-01 08:20:00'),
(4, 'Sidebar left promotion', '/images/banners/sidebar-best-seller.jpg', '/products', 1, 'site_left_sidebar_banner', true, '2026-05-01 00:00:00', NULL, 'WEB', '2026-05-01 08:30:00', '2026-05-01 08:30:00'),
(5, 'Sidebar right promotion', '/images/banners/sidebar-best-seller.jpg', '/products', 1, 'site_right_sidebar_banner', true, '2026-05-01 00:00:00', NULL, 'WEB', '2026-05-01 08:40:00', '2026-05-01 08:40:00'),
(6, 'Bán chạy section banner', 'https://i0.wp.com/www.comicbookrevolution.com/wp-content/uploads/2023/12/transformers-4-previw-banner.jpg', '/products', 1, 'home_section_01_banner', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 08:50:00', '2026-05-01 08:50:00'),
(7, 'Custom service primary', 'https://images.unsplash.com/photo-1730110206448-10297c1902bd?q=80&w=800&auto=format&fit=crop', '/services', 1, 'home_custom_service_image_primary', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:00:00', '2026-05-01 09:00:00'),
(8, 'Custom service secondary', 'https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=800&auto=format&fit=crop', '/services', 1, 'home_custom_service_image_secondary', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:10:00', '2026-05-01 09:10:00'),
(9, 'Custom service tertiary', 'https://images.unsplash.com/photo-1532581140115-3e355d1ed1de?q=80&w=600&auto=format&fit=crop', '/services', 1, 'home_custom_service_image_tertiary', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:20:00', '2026-05-01 09:20:00'),
(10, 'Marvel category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/marvel', 1, 'home_category_card_01', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:30:00', '2026-05-01 09:30:00'),
(11, 'DC category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/dc', 1, 'home_category_card_02', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:40:00', '2026-05-01 09:40:00'),
(12, 'Hot Wheels category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/hot wheels', 1, 'home_category_card_03', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:50:00', '2026-05-01 09:50:00'),
(13, 'Transformer category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/transformer', 1, 'home_category_card_04', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:00:00', '2026-05-01 10:00:00'),
(14, 'Naruto category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/naruto', 1, 'home_category_card_05', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:10:00', '2026-05-01 10:10:00'),
(15, 'Pacific Rim category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/pacific rim', 1, 'home_category_card_06', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:20:00', '2026-05-01 10:20:00'),
(16, 'Dụng Cụ section banner', 'https://tooltechvietnam.com/wp-content/uploads/2023/03/handtools.jpg', '/products', 1, 'home_section_02_banner', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:30:00', '2026-05-01 10:30:00'),
(17, 'Promo grid top left', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/products', 1, 'home_promo_grid_top_left', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:40:00', '2026-05-01 10:40:00'),
(18, 'Promo grid bottom left', 'https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg', '/products', 1, 'home_promo_grid_bottom_left', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:50:00', '2026-05-01 10:50:00'),
(19, 'Promo grid top right', 'https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg', '/products', 1, 'home_promo_grid_top_right', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 11:00:00', '2026-05-01 11:00:00'),
(20, 'Promo grid bottom right', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/products', 1, 'home_promo_grid_bottom_right', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 11:10:00', '2026-05-01 11:10:00'),
(21, 'Hotwheels section banner', 'https://images.unsplash.com/photo-1551522435-a13afa10f103?q=80&w=1600&auto=format&fit=crop', '/products', 1, 'home_section_03_banner', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 11:20:00', '2026-05-01 11:20:00'),
(22, 'Sale section banner', 'https://img.magnific.com/free-vector/modern-black-friday-holiday-sale-offer-banner-get-30-percent-price-drop-vector_1017-47794.jpg?semt=ais_hybrid&w=740&q=80', '/products', 1, 'home_section_04_banner', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 11:30:00', '2026-05-01 11:30:00')
ON DUPLICATE KEY UPDATE
title = VALUES(title),
image_url = VALUES(image_url),
link_url = VALUES(link_url),
display_order = VALUES(display_order),
position = VALUES(position),
is_active = VALUES(is_active),
device_type = VALUES(device_type);

-- ========================
-- 11. REVIEWS
-- ========================
INSERT INTO reviews (id, account_id, product_id, order_id, rating, content, image_urls, status, admin_reply, replied_by, replied_at, created_at, updated_at) VALUES
(1, 4, 1001, 3, 5, 'Sản phẩm rất tuyệt! Da mịn màng, sạch sâu mà không bị khô. Mùi hương dễ chịu, tạo bọt tốt. Đã dùng được 2 tuần và thấy hiệu quả rõ rệt.', '["https://placehold.co/600x400/00618e/ffffff?text=Review+Sodu+Gentle+1","https://placehold.co/600x400/005f9c/ffffff?text=Review+Sodu+Gentle+2"]', 'PUBLISHED', 'Cảm ơn bạn đã tin dùng sản phẩm của Sodu! Chúng tôi rất vui vì bạn hài lòng với trải nghiệm.', 1, '2026-06-21 10:00:00', '2026-06-20 18:00:00', '2026-06-21 10:00:00'),
(2, 5, 1001, 3, 4, 'Sản phẩm dùng tốt, sạch và thơm. Mình thuộc da dầu nên hơi lo nhưng dùng ổn. Giá cả hợp lý.', '[]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-22 09:30:00', '2026-06-22 09:30:00'),
(3, 4, 1002, 3, 5, 'Kem chống nắng rất mỏng nhẹ, không bết dính. Thấm nhanh và không để lại vệt trắng. Rất thích hợp cho da dầu mụn.', '["https://placehold.co/600x400/5a4bb4/ffffff?text=Aurora+Sunscreen+Review"]', 'PUBLISHED', NULL, NULL, NULL, '2026-06-23 14:00:00', '2026-06-23 14:00:00'),
(4, 5, 1002, 3, 2, 'Không hợp với da mình, bị kích ứng nhẹ sau khi dùng. Có thể do da nhạy cảm. Sẽ dùng thử thêm vài lần nữa.', '[]', 'HIDDEN', NULL, NULL, NULL, '2026-06-24 11:00:00', '2026-06-24 11:00:00')
ON DUPLICATE KEY UPDATE
account_id = VALUES(account_id),
product_id = VALUES(product_id),
order_id = VALUES(order_id),
rating = VALUES(rating),
content = VALUES(content),
image_urls = VALUES(image_urls),
status = VALUES(status),
admin_reply = VALUES(admin_reply),
replied_by = VALUES(replied_by),
replied_at = VALUES(replied_at),
updated_at = VALUES(updated_at);

-- ========================
-- 12. VOUCHERS
-- ========================
INSERT INTO vouchers (id, code, name, type, slot, scope, geo_scope, value, max_discount_amount, min_order_value, usage_limit, used_count, auto_apply, active, deleted, start_date, end_date, created_at, updated_at) VALUES
(1, 'SOBUAUTO5', 'Tự động giảm 5% toàn đơn từ 200k', 'DISCOUNT_PERCENT', 'ORDER', 'ALL', 'ALL', 5.00, 50000.00, 200000.00, 1000, 0, true, true, false, '2026-01-01 00:00:00', '2026-12-31 23:59:59', NOW(), NOW()),
(2, 'HANOIFREE', 'Miễn phí vận chuyển 11 quận nội thành Hà Nội', 'FREE_SHIP', 'SHIPPING', 'ALL', 'HANOI_CENTER', 30000.00, 30000.00, 150000.00, 2000, 0, true, true, false, '2026-01-01 00:00:00', '2026-12-31 23:59:59', NOW(), NOW()),
(3, 'SKINCARE10', 'Giảm 10% cho danh mục Chăm sóc da', 'DISCOUNT_PERCENT', 'ITEM', 'CATEGORY', 'ALL', 10.00, 50000.00, 200000.00, 500, 0, false, true, false, '2026-01-01 00:00:00', '2026-12-31 23:59:59', NOW(), NOW()),
(4, 'PROMOVIP15', 'Giảm 15% sản phẩm Sodu Gentle', 'DISCOUNT_PERCENT', 'ITEM', 'PRODUCT', 'ALL', 15.00, 60000.00, 100000.00, 300, 0, false, true, false, '2026-01-01 00:00:00', '2026-12-31 23:59:59', NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
type = VALUES(type),
slot = VALUES(slot),
scope = VALUES(scope),
geo_scope = VALUES(geo_scope),
value = VALUES(value),
max_discount_amount = VALUES(max_discount_amount),
min_order_value = VALUES(min_order_value),
usage_limit = VALUES(usage_limit),
auto_apply = VALUES(auto_apply),
active = VALUES(active),
deleted = VALUES(deleted),
updated_at = VALUES(updated_at);

INSERT IGNORE INTO voucher_category_ids (voucher_id, category_id) VALUES
(3, 100);

INSERT IGNORE INTO voucher_product_ids (voucher_id, product_id) VALUES
(4, 1001);

-- ========================
-- 13. ARTICLES / BLOG (SEO)
-- ========================
INSERT INTO articles (id, title, slug, seo_title, meta_description, thumbnail_url, thumbnail_alt, excerpt, content, author_name, category, status, published_at, created_at, updated_at) VALUES
(1, 'Bí quyết chăm sóc da nhạy cảm vào mùa hè đúng cách', 'bi-quyet-cham-soc-da-nhay-cam-vao-mua-he-dung-cach', 'Bí Quyết Chăm Sóc Da Nhạy Cảm Mùa Hè 2026 | Sobu Blog', 'Hướng dẫn chi tiết các bước skincare cho làn da nhạy cảm khi thời tiết nắng nóng, giúp da luôn thông thoáng và khỏe mạnh.', 'https://placehold.co/800x450/00618e/ffffff?text=Skincare+Mua+He', 'Bí quyết chăm sóc da nhạy cảm mùa hè', 'Làn da nhạy cảm rất dễ bị kích ứng khi thời tiết nắng gắt. Hãy cùng Sobu tìm hiểu các bước chăm sóc chuẩn khoa học.', '<p>Mùa hè mang đến ánh nắng gay gắt và nhiệt độ cao, khiến tuyến bã nhờn hoạt động mạnh mẽ...</p>', 'Chuyên gia Da liễu Sobu', 'Chăm sóc da', 'PUBLISHED', '2026-06-01 08:00:00', '2026-06-01 08:00:00', '2026-06-01 08:00:00'),
(2, 'Top 5 loại kem chống nắng phổ rộng tốt nhất 2026', 'top-5-loai-kem-chong-nang-pho-rong-tot-nhat-2026', 'Top 5 Kem Chống Nắng Phổ Rộng Tốt Nhất 2026 | Sobu Blog', 'Đánh giá top 5 kem chống nắng phổ rộng bảo vệ da tối ưu, không để lại vệt trắng và kiềm dầu hiệu quả.', 'https://placehold.co/800x450/5a4bb4/ffffff?text=Top+Kem+Chong+Nang', 'Top 5 kem chống nắng phổ rộng 2026', 'Tổng hợp đánh giá chi tiết top 5 dòng kem chống nắng phổ rộng được tin dùng nhất năm 2026.', '<p>Kem chống nắng phổ rộng là vật bất ly thân giúp bảo vệ da trước cả tia UVA và UVB...</p>', 'Ban Biên Tập Sobu', 'Kinh nghiệm', 'PUBLISHED', '2026-06-05 09:30:00', '2026-06-05 09:30:00', '2026-06-05 09:30:00')
ON DUPLICATE KEY UPDATE
title = VALUES(title),
slug = VALUES(slug),
seo_title = VALUES(seo_title),
meta_description = VALUES(meta_description),
thumbnail_url = VALUES(thumbnail_url),
thumbnail_alt = VALUES(thumbnail_alt),
excerpt = VALUES(excerpt),
content = VALUES(content),
author_name = VALUES(author_name),
category = VALUES(category),
status = VALUES(status),
published_at = VALUES(published_at),
updated_at = VALUES(updated_at);

