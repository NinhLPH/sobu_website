-- ========================
-- ROLE
-- ========================
INSERT INTO role (name, description) VALUES 
('ADMIN', 'Quản trị viên hệ thống'),
('USER', 'Khách hàng mua sắm'),
('MANAGER','Quản lý'),
('STAFF', 'Nhân viên kho và đơn hàng')
ON DUPLICATE KEY UPDATE description = VALUES(description);

