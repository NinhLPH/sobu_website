# Backend

## Quick Start: login nhanh, cấp quyền, sync categories và product

Mục này dành cho môi trường local/dev với cấu hình mặc định trong [application-dev.properties](D:\Code\sobu_website\backend\src\main\resources\application-dev.properties): MySQL `sodu`, user `root`, password `1234`.

### 1. Tạo tài khoản

Đăng ký user mới:

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "123456",
    "fullName": "Local Admin",
    "phone": "0900000001"
  }'
```

Mặc định account mới tạo sẽ:
- có `status = INACTIVE`
- có `role_id = 2` theo code trong `AuthService` tức role mặc định không phải admin

### 2. Kích hoạt nhanh và cấp quyền admin

Repo hiện chưa có endpoint quản trị role/account public, nên cách nhanh nhất ở local là cập nhật trực tiếp DB.

Seed role nằm trong [sampleData.sql](D:\Code\sobu_website\backend\src\main\resources\sampleData.sql):
- `1 = ADMIN`
- `2 = USER`
- `3 = MANAGER`
- `4 = STAFF`

Chạy SQL:

```sql
USE sodu;

UPDATE account
SET status = 'ACTIVE',
    role_id = 1
WHERE email = 'admin@example.com';
```

Nếu chỉ muốn login test mà chưa cần quyền admin, có thể chỉ update `status = 'ACTIVE'`.

### 3. Login lấy access token

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "123456"
  }'
```

Lấy `data.accessToken` từ response và gắn vào header:

```text
Authorization: Bearer <accessToken>
```

Lưu ý:
- `SecurityConfig` hiện cho phép public chỉ các route `/api/auth/**`, Swagger và `/api/public/**`
- các endpoint sync yêu cầu user đã đăng nhập
- về vận hành nên dùng tài khoản `ADMIN` để gọi sync

### 4. Sync categories

Theo log runtime hiện tại, endpoint category sync là:

```bash
curl -X POST http://localhost:8081/api/admin/categories/sync \
  -H "Authorization: Bearer <accessToken>"
```

Nếu sync thành công, service sẽ log tổng số record theo dạng:

```text
Nhanh sync done. total=..., success=..., failed=...
```

### 5. Sync products

Theo integration test đã build của project, endpoint product sync là:

```bash
curl -X POST http://localhost:8081/admin/products/sync \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json"
```

Lưu ý hiện tại route sync của `categories` và `products` đang không đồng nhất:
- category: `/api/admin/categories/sync`
- product: `/admin/products/sync`

Khi test thủ công nên gọi đúng từng route như trên.

### 6. Thứ tự chạy khuyến nghị

Nên chạy theo thứ tự:

1. Sync categories trước
2. Sync products sau

Lý do: product thường phụ thuộc vào dữ liệu category/brand đã có sẵn trong DB trước đó.

### 7. Kiểm tra nhanh sau khi sync

Có thể kiểm tra trực tiếp DB:

```sql
SELECT COUNT(*) FROM categories;
SELECT COUNT(*) FROM products;
```

Nếu category sync lỗi foreign key `parent_id`, kiểm tra lại dữ liệu cha-con từ upstream và xem log của `CategorySyncService`.
