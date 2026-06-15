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
INSERT INTO banners (
id, title, image_url, link_url, display_order, position, is_active,
start_date, end_date, device_type, created_at, updated_at
) VALUES
(1, 'Sodu Beauty - Chăm da mùa hè', '/images/banners/home-top-summer.jpg', '/products?categoryId=102', 1, 'HOME_TOP', true, '2026-05-01 00:00:00', '2026-08-31 23:59:59', 'ALL', '2026-05-01 08:00:00', '2026-05-01 08:00:00'),
(2, 'Ưu đãi son môi Melia', '/images/banners/lipstick-promo.jpg', '/products?categoryId=201', 2, 'HOME_MIDDLE', true, '2026-05-01 00:00:00', '2026-06-30 23:59:59', 'WEB', '2026-05-01 08:10:00', '2026-05-01 08:10:00'),
(3, 'Gợi ý sản phẩm bán chạy', '/images/banners/sidebar-best-seller.jpg', '/products', 1, 'PRODUCT_SIDEBAR', true, '2026-05-01 00:00:00', NULL, 'ALL', '2026-05-01 08:20:00', '2026-05-01 08:20:00')
ON DUPLICATE KEY UPDATE
title = VALUES(title),
image_url = VALUES(image_url),
link_url = VALUES(link_url),
display_order = VALUES(display_order),
position = VALUES(position),
is_active = VALUES(is_active),
start_date = VALUES(start_date),
end_date = VALUES(end_date),
device_type = VALUES(device_type),
updated_at = VALUES(updated_at);

INSERT INTO website_configurations (
id, config_key, config_value, type, group_name, description, is_public, is_active, created_at, updated_at
) VALUES
(1, 'site_name', 'Sodu Beauty', 'text', 'general', 'Tên website hiển thị ở giao diện.', true, true, '2026-05-01 08:00:00', '2026-05-01 08:00:00'),
(2, 'primary_color', '#D84F7A', 'color', 'theme', 'Màu thương hiệu chính.', true, true, '2026-05-01 08:00:00', '2026-05-01 08:00:00'),
(3, 'free_shipping_threshold', '500000', 'number', 'commerce', 'Ngưỡng miễn phí vận chuyển.', true, true, '2026-05-01 08:00:00', '2026-05-01 08:00:00'),
(4, 'support_hotline', '1900 636 999', 'text', 'support', 'Số hotline hỗ trợ khách hàng.', true, true, '2026-05-01 08:00:00', '2026-05-01 08:00:00'),
(5, 'homepage_sections', '{"sections":["banners","categories","bestSellers","newArrivals"]}', 'json', 'homepage', 'Cấu hình thứ tự block trang chủ.', true, true, '2026-05-01 08:00:00', '2026-05-01 08:00:00')
ON DUPLICATE KEY UPDATE
config_value = VALUES(config_value),
type = VALUES(type),
group_name = VALUES(group_name),
description = VALUES(description),
is_public = VALUES(is_public),
is_active = VALUES(is_active),
updated_at = VALUES(updated_at);


