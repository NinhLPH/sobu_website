-- ========================
-- ROLE
-- ========================
INSERT INTO role (name, description) VALUES 
('ADMIN', 'Quản trị viên hệ thống'),
('USER', 'Khách hàng mua sắm'),
('MANAGER','Quản lý'),
('STAFF', 'Nhân viên kho và đơn hàng')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ========================
-- LOYALTY
-- ========================
INSERT INTO loyalty_tiers (id, name, min_total_money, discount_rate) VALUES
(1, 0, 0, 0),
(2, 1, 5000000, 3),
(3, 2, 15000000, 7)
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
-- ACCOUNT / CUSTOMER
-- password for all sample accounts: password
-- ========================
INSERT INTO account (id, role_id, email, phone, password_hash, full_name, status) VALUES
(1, (SELECT id FROM role WHERE name = 'ADMIN'), 'admin@sodu.vn', '0901000001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Quản trị Sodu', 'ACTIVE'),
(2, (SELECT id FROM role WHERE name = 'MANAGER'), 'manager@sodu.vn', '0901000002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Nguyễn Minh Quản', 'ACTIVE'),
(3, (SELECT id FROM role WHERE name = 'STAFF'), 'staff@sodu.vn', '0901000003', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Trần Thu Kho', 'ACTIVE'),
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
-- CATEGORY / BRAND
-- ========================
INSERT INTO categories (id, parent_id, code, name, sort_order, image, content, status) VALUES
(100, NULL, 'SKINCARE', 'Chăm sóc da', 1, '/images/categories/skincare.jpg', 'Sản phẩm chăm sóc da mặt và cơ thể.', 1),
(101, 100, 'CLEANSER', 'Sữa rửa mặt', 1, '/images/categories/cleanser.jpg', 'Làm sạch dịu nhẹ hằng ngày.', 1),
(102, 100, 'SUNSCREEN', 'Kem chống nắng', 2, '/images/categories/sunscreen.jpg', 'Bảo vệ da trước tia UV.', 1),
(200, NULL, 'MAKEUP', 'Trang điểm', 2, '/images/categories/makeup.jpg', 'Sản phẩm trang điểm cá nhân.', 1),
(201, 200, 'LIPSTICK', 'Son môi', 1, '/images/categories/lipstick.jpg', 'Son môi nhiều tông màu.', 1)
ON DUPLICATE KEY UPDATE
parent_id = VALUES(parent_id),
code = VALUES(code),
name = VALUES(name),
sort_order = VALUES(sort_order),
image = VALUES(image),
content = VALUES(content),
status = VALUES(status);

INSERT INTO brands (id, code, name, status, parent_id, created_at) VALUES
(10, 'SODU', 'Sodu Beauty', 1, NULL, '2026-05-01 08:00:00'),
(11, 'SODU-LAB', 'Sodu Lab', 1, 10, '2026-05-01 08:05:00'),
(20, 'AURORA', 'Aurora Skincare', 1, NULL, '2026-05-02 09:00:00'),
(30, 'MELIA', 'Melia Cosmetics', 1, NULL, '2026-05-03 10:00:00')
ON DUPLICATE KEY UPDATE
code = VALUES(code),
name = VALUES(name),
status = VALUES(status),
parent_id = VALUES(parent_id),
created_at = VALUES(created_at);

-- ========================
-- PRODUCT
-- ========================
INSERT INTO products (
id, external_id, parent_id, code, barcode, name, other_name, status,
category_id, category_name, internal_category_id, internal_category_name,
brand_id, brand_name, type_id, type_name, supplier_id, supplier_name, supplier_phone,
retail_price, import_price, wholesale_price, old_price, avg_cost, vat,
avatar_image, length, width, height, weight, country_name,
stock_remain, stock_available, description, content, created_at, updated_at, raw_data
) VALUES
(1001, 9001001, NULL, 'SD-CLEANSER-120', '8938500000011', 'Sữa rửa mặt Sodu Gentle 120ml', 'Gentle Cleanser', 'ACTIVE',
101, 'Sữa rửa mặt', 101, 'Sữa rửa mặt',
10, 'Sodu Beauty', 1, 'Sản phẩm thường', 501, 'Sodu Distribution', '02873000001',
189000, 98000, 155000, 229000, 102000, 8,
'/images/products/sd-cleanser-120.jpg', 12, 5, 5, 180, 'Việt Nam',
120, 110, 'Sữa rửa mặt dịu nhẹ cho da thường và da nhạy cảm.', 'Làm sạch bụi bẩn, dầu thừa mà không gây khô căng.', '2026-05-10 09:00:00', '2026-05-20 11:00:00', '{"source":"sample"}'),
(1002, 9001002, NULL, 'AR-SUNSCREEN-50', '8938500000028', 'Kem chống nắng Aurora SPF50 PA++++ 50ml', 'Aurora Sunscreen', 'ACTIVE',
102, 'Kem chống nắng', 102, 'Kem chống nắng',
20, 'Aurora Skincare', 1, 'Sản phẩm thường', 502, 'Aurora Việt Nam', '02873000002',
329000, 190000, 285000, 389000, 198000, 8,
'/images/products/ar-sunscreen-50.jpg', 14, 4, 4, 120, 'Hàn Quốc',
75, 70, 'Kem chống nắng phổ rộng, kết cấu mỏng nhẹ.', 'Phù hợp dùng hằng ngày dưới lớp trang điểm.', '2026-05-11 10:00:00', '2026-05-20 11:05:00', '{"source":"sample"}'),
(1003, 9001003, NULL, 'ML-LIP-M01', '8938500000035', 'Son lì Melia Velvet màu Rose Mood', 'Melia Velvet Rose Mood', 'ACTIVE',
201, 'Son môi', 201, 'Son môi',
30, 'Melia Cosmetics', 1, 'Sản phẩm thường', 503, 'Melia Official', '02873000003',
249000, 120000, 210000, 299000, 128000, 8,
'/images/products/ml-lip-m01.jpg', 9, 2, 2, 60, 'Ý',
210, 205, 'Son lì mềm môi, màu rose mood dễ dùng.', 'Chất son mịn, bám màu tốt trong nhiều giờ.', '2026-05-12 13:30:00', '2026-05-20 11:10:00', '{"source":"sample"}')
ON DUPLICATE KEY UPDATE
external_id = VALUES(external_id),
code = VALUES(code),
barcode = VALUES(barcode),
name = VALUES(name),
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
avg_cost = VALUES(avg_cost),
vat = VALUES(vat),
avatar_image = VALUES(avatar_image),
stock_remain = VALUES(stock_remain),
stock_available = VALUES(stock_available),
description = VALUES(description),
content = VALUES(content),
updated_at = VALUES(updated_at),
raw_data = VALUES(raw_data);

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

INSERT INTO product_images (id, product_id, url) VALUES
(1, 1001, '/images/products/sd-cleanser-120-1.jpg'),
(2, 1001, '/images/products/sd-cleanser-120-2.jpg'),
(3, 1002, '/images/products/ar-sunscreen-50-1.jpg'),
(4, 1003, '/images/products/ml-lip-m01-1.jpg')
ON DUPLICATE KEY UPDATE
product_id = VALUES(product_id),
url = VALUES(url);

INSERT INTO product_videos (id, product_id, title, src) VALUES
(1, 1001, 'Hướng dẫn dùng Sodu Gentle Cleanser', '/videos/products/sd-cleanser-demo.mp4'),
(2, 1002, 'Test kết cấu Aurora Sunscreen', '/videos/products/ar-sunscreen-texture.mp4')
ON DUPLICATE KEY UPDATE
product_id = VALUES(product_id),
title = VALUES(title),
src = VALUES(src);

-- ========================
-- REQUEST / ORDER
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
(2, 1, 'APPROVE_REQUEST', 'REVIEWING', 'APPROVED', 'manager@sodu.vn', 'Đã xác nhận tồn kho và duyệt yêu cầu.', '2026-05-18 09:20:00'),
(3, 3, 'START_REVIEW', 'PENDING', 'REVIEWING', 'staff@sodu.vn', 'Nhân viên bắt đầu tìm sản phẩm phù hợp.', '2026-05-20 09:00:00')
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
0, '2026-05-19 14:10:00', '2026-05-20 09:10:00')
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
(3, 2, '9001003', 'Son lì Melia Velvet màu Rose Mood', 'Gói quà', 249000, 0, 2)
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
-- UI / CONFIGURATION
-- ========================
INSERT INTO static_pages (
id, slug, title, html_content, is_published, created_at, updated_at
) VALUES
(1, 'about', 'About', '<h1>About SOBU</h1><p>SOBU Studio serves collectors with model products, preorder support, sourcing, and custom services.</p>', true, NOW(), NOW()),
(2, 'privacy-policy', 'Privacy Policy', '<h1>Privacy Policy</h1><p>SOBU collects only the information needed to process orders, support requests, and improve customer service.</p>', true, NOW(), NOW()),
(3, 'terms', 'Terms', '<h1>Terms</h1><p>By using SOBU services, customers agree to provide accurate order information and follow the published payment and delivery policies.</p>', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
slug = slug;

INSERT INTO banners (
id, title, image_url, link_url, display_order, position, is_active,
start_date, end_date, device_type, created_at, updated_at
) VALUES
(1, 'SOBU STUDIO', 'https://images.unsplash.com/photo-1614200187524-dc4b892acf16?q=80&w=2000&auto=format&fit=crop', '/products', 1, 'home_hero_carousel', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 08:00:00', '2026-05-01 08:00:00'),
(2, 'HOT WHEELS', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/products', 2, 'home_hero_carousel', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 08:10:00', '2026-05-01 08:10:00'),
(3, 'MECHA & GUNDAM', 'https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=800&auto=format&fit=crop', '/services', 3, 'home_hero_carousel', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 08:20:00', '2026-05-01 08:20:00'),
(4, 'Sidebar left promotion', '/images/banners/sidebar-best-seller.jpg', '/products', 1, 'site_left_sidebar_banner', true, '2026-05-01 00:00:00', NULL, 'WEB', '2026-05-01 08:30:00', '2026-05-01 08:30:00'),
(5, 'Sidebar right promotion', '/images/banners/sidebar-best-seller.jpg', '/products', 1, 'site_right_sidebar_banner', true, '2026-05-01 00:00:00', NULL, 'WEB', '2026-05-01 08:40:00', '2026-05-01 08:40:00'),
(6, 'Ban chay section banner', 'https://i0.wp.com/www.comicbookrevolution.com/wp-content/uploads/2023/12/transformers-4-previw-banner.jpg', '/products', 1, 'home_section_01_banner', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 08:50:00', '2026-05-01 08:50:00'),
(7, 'Custom service primary', 'https://images.unsplash.com/photo-1730110206448-10297c1902bd?q=80&w=800&auto=format&fit=crop', '/services', 1, 'home_custom_service_image_primary', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:00:00', '2026-05-01 09:00:00'),
(8, 'Custom service secondary', 'https://images.unsplash.com/photo-1618331835717-801e976710b2?q=80&w=800&auto=format&fit=crop', '/services', 1, 'home_custom_service_image_secondary', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:10:00', '2026-05-01 09:10:00'),
(9, 'Custom service tertiary', 'https://images.unsplash.com/photo-1532581140115-3e355d1ed1de?q=80&w=600&auto=format&fit=crop', '/services', 1, 'home_custom_service_image_tertiary', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:20:00', '2026-05-01 09:20:00'),
(10, 'Marvel category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/marvel', 1, 'home_category_card_01', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:30:00', '2026-05-01 09:30:00'),
(11, 'DC category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/dc', 1, 'home_category_card_02', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:40:00', '2026-05-01 09:40:00'),
(12, 'Hot Wheels category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/hot wheels', 1, 'home_category_card_03', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 09:50:00', '2026-05-01 09:50:00'),
(13, 'Transformer category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/transformer', 1, 'home_category_card_04', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:00:00', '2026-05-01 10:00:00'),
(14, 'Naruto category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/naruto', 1, 'home_category_card_05', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:10:00', '2026-05-01 10:10:00'),
(15, 'Pacific Rim category card', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/category/pacific rim', 1, 'home_category_card_06', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:20:00', '2026-05-01 10:20:00'),
(16, 'Dung Cu section banner', 'https://tooltechvietnam.com/wp-content/uploads/2023/03/handtools.jpg', '/products', 1, 'home_section_02_banner', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:30:00', '2026-05-01 10:30:00'),
(17, 'Promo grid top left', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/products', 1, 'home_promo_grid_top_left', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:40:00', '2026-05-01 10:40:00'),
(18, 'Promo grid bottom left', 'https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg', '/products', 1, 'home_promo_grid_bottom_left', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 10:50:00', '2026-05-01 10:50:00'),
(19, 'Promo grid top right', 'https://images-na.ssl-images-amazon.com/images/I/71NGNYdc2NL.jpg', '/products', 1, 'home_promo_grid_top_right', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 11:00:00', '2026-05-01 11:00:00'),
(20, 'Promo grid bottom right', 'https://storage.ghost.io/c/81/4f/814f42c9-9554-47a0-a5c0-499b2f9606cf/content/images/2024/09/2024-hot-wheels-poster-4-0.jpg', '/products', 1, 'home_promo_grid_bottom_right', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 11:10:00', '2026-05-01 11:10:00'),
(21, 'Hotwheels section banner', 'https://images.unsplash.com/photo-1551522435-a13afa10f103?q=80&w=1600&auto=format&fit=crop', '/products', 1, 'home_section_03_banner', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 11:20:00', '2026-05-01 11:20:00'),
(22, 'Sale section banner', 'https://img.magnific.com/free-vector/modern-black-friday-holiday-sale-offer-banner-get-30-percent-price-drop-vector_1017-47794.jpg?semt=ais_hybrid&w=740&q=80', '/products', 1, 'home_section_04_banner', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 11:30:00', '2026-05-01 11:30:00')
ON DUPLICATE KEY UPDATE
id = id;

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
('working_hours', '09:00 - 21:00, Thứ 2 - Chủ nhật', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('footer_greeting_text', 'SOBU đồng hành cùng cộng đồng collector trong từng đơn hàng và yêu cầu đặc biệt.', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('copyright_text', '(c) 2026 SOBU Studio. All rights reserved.', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('newsletter_enabled', 'false', 'boolean_type', 'GENERAL', NULL, true, true, NOW(), NOW()),
('newsletter_description', 'Nhan thong tin ve san pham moi, hang sap ve va uu dai rieng cho collector.', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('newsletter_submit_label', 'Dang ky', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('footer_company_links', '[{"label":"Gioi thieu","href":"/about"},{"label":"Dich vu","href":"/services"},{"label":"Blog","href":"/blog"}]', 'json', 'FOOTER', NULL, true, true, NOW(), NOW()),
('footer_help_links', '[{"label":"San pham","href":"/products"},{"label":"Yeu cau tim hang","href":"/request"},{"label":"Lien he","href":"/contact"}]', 'json', 'FOOTER', NULL, true, true, NOW(), NOW()),
('legal_links', '[{"label":"Dieu khoan","href":"/terms"},{"label":"Bao mat","href":"/privacy"}]', 'json', 'FOOTER', NULL, true, true, NOW(), NOW()),
('home_section_01_title', 'BAN CHAY', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_01_subtitle', 'Giao Hang Toan Quoc', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_01_label', 'Xem them', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_01_cta_url', '/products', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_custom_service_title', 'DICH VU DO MO HINH SO 1 VIET NAM', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_custom_service_badges', '["Do Led cam ung","Son mo hinh chuan phim","Custom theo y thich"]', 'json', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_custom_service_cta_label', 'CUSTOM NGAY', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_custom_service_cta_url', '/services', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_02_title', 'MO HINH CUSTOM', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_02_subtitle', 'Giao Hang Toan Quoc', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_02_cta_label', 'Xem them', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_02_cta_url', '/products', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_category_title', 'The loai mo hinh', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_category_cards', '[{"label":"Marvel","href":"/category/marvel","bannerPosition":"home_category_card_01"},{"label":"DC","href":"/category/dc","bannerPosition":"home_category_card_02"},{"label":"Hot Wheels","href":"/category/hot wheels","bannerPosition":"home_category_card_03"},{"label":"Transformer","href":"/category/transformer","bannerPosition":"home_category_card_04"},{"label":"Naruto","href":"/category/naruto","bannerPosition":"home_category_card_05"},{"label":"Pacific Rim","href":"/category/pacific rim","bannerPosition":"home_category_card_06"}]', 'json', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_03_title', 'Dung Cu', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_03_subtitle', 'Giao Hang Toan Quoc', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_03_cta_label', 'Xem them', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_03_cta_url', '/products', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_04_title', 'Hotwheels', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_04_subtitle', 'Giao Hang Toan Quoc', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_04_cta_label', 'Xem them', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_04_cta_url', '/products', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_05_title', 'Giam gia cuc manh', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_05_subtitle', '', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_05_cta_label', 'Xem them', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_section_05_cta_url', '/products', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_news_title', 'Tin Tuc', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_news_more_label', 'MORE', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_news_more_url', '/blog', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_testimonials_title', 'Danh gia tu khach hang', 'text', 'HOME_SECTION', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_left_title', 'HOT WHEELS', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_left_description', 'KHAM PHA NHUNG MAU XE MO HINH HOT NHAT DANH CHO NGUOI DAM ME TOC DO.', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_left_cta_label', 'Xem them', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_left_cta_url', '/products', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_title', 'Suu tam huyen thoai', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_description', 'SUU TAM NHUNG MAU XE HUYEN THOAI - TU SIEU XE HIEN DAI DEN CLASSIC CO DIEN.', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_cta_label', '', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_left_cta_url', '/products', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_right_title', 'Limited Edition Cars', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_right_description', 'DISCOVER LIMITED EDITION CARS AND EXCLUSIVE RELEASES FOR TRUE COLLECTORS.', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_right_cta_label', '', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_top_right_cta_url', '/products', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_title', 'GIFT FOR COLLECTORS', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_description', 'MON QUA HOAN HAO CHO NGUOI YEU XE VA DAM ME MO HINH.', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_cta_label', 'Xem them', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_promo_grid_bottom_right_cta_url', '/products', 'text', 'HOME_PROMO', NULL, true, true, NOW(), NOW()),
('home_partners_title', 'Doi tac chien luoc & Thuong hieu dong hanh', 'text', 'HOME_PARTNER', NULL, true, true, NOW(), NOW()),
('home_partner_brands', '[{"name":"BANDAI","logoUrl":"https://placehold.co/180x60/e60012/ffffff?text=BANDAI"},{"name":"HOT TOYS","logoUrl":"https://placehold.co/180x60/111111/f1b82d?text=HOT+TOYS"},{"name":"TAMIYA","logoUrl":"https://placehold.co/180x60/0054a6/ffffff?text=TAMIYA"},{"name":"LEGO","logoUrl":"https://placehold.co/180x60/ffd500/000000?text=LEGO"},{"name":"MATTEL","logoUrl":"https://placehold.co/180x60/e5142a/ffffff?text=MATTEL"},{"name":"HASBRO","logoUrl":"https://placehold.co/180x60/0072ce/ffffff?text=HASBRO"}]', 'json', 'HOME_PARTNER', NULL, true, true, NOW(), NOW()),('maintenance_mode_enabled', 'false', 'boolean_type', 'GENERAL', NULL, true, true, NOW(), NOW()),
('maintenance_message', 'Website đang được bảo trì. Vui lòng quay lại sau.', 'text', 'GENERAL', NULL, true, true, NOW(), NOW()),
('social_links', '{"facebook":"","instagram":"","tiktok":"","youtube":"","zalo":""}', 'json', 'SOCIAL', NULL, true, true, NOW(), NOW()),
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


