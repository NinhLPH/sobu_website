# Soduo API Doc cho Frontend

Tài liệu này viết lại theo kiểu "FE đọc là gọi được ngay", không giả định bạn biết backend.

## 1. Đọc nhanh trong 2 phút

### Base URL

- Local: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Auth có bắt buộc không?

- `KHONG can token` với:
  - `/api/auth/**`
  - `/api/public/**`
  - `/api/v1/public/**`
  - `/api/nhanh/**`
- `CAN token` với tất cả route còn lại.

Header cần gửi:

```http
Authorization: Bearer <accessToken>
```

### API trả về theo format nào?

Phần lớn API trả theo wrapper này:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "OK",
  "data": {},
  "error": null,
  "timestamp": "2026-05-26T10:00:00"
}
```

Nhưng có 3 ngoại lệ quan trọng:

1. `GET /api/public/products`
2. `GET/POST /api/public/products/search`
3. `GET /api/public/products/{id}`

Những API public catalog ở trên trả thẳng object/list, `KHONG` bọc trong `ApiResponseDTO`.

### Error format

Khi lỗi, backend thường trả:

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid request",
  "error": "BAD_REQUEST",
  "timestamp": "2026-05-26T10:00:00"
}
```

### Các route có nhiều alias

Một số API hỗ trợ cả version cũ và mới:

- Public catalog: `/api/public/...` và `/api/v1/public/...`
- Request: `/api/requests/...`, `/api/request/...`, `/api/v1/requests/...`, `/api/v1/request/...`
- Orders: `/api/orders/...` và `/api/v1/orders/...`
- Admin requests: `/api/admin/requests/...` và `/api/v1/admin/requests/...`

Để dễ nhớ, tài liệu này ưu tiên ghi route ngắn hơn khi có thể.

## 2. 3W1H cho từng nhóm API

## 2.1 Authentication

### What

Đăng nhập, đăng ký, kích hoạt tài khoản, refresh token, logout.

### Why

FE cần nhóm API này để lấy `accessToken` và biết user hiện tại là ai, role gì.

### When

- Gọi `login` khi user submit form login
- Gọi `refresh-token` khi access token hết hạn
- Gọi `register` khi user tạo tài khoản
- Gọi `activate` khi user bấm link trong email
- Gọi `logout` khi user đăng xuất

### How

| Endpoint | Method | Auth | Body / Params | Trả về |
| --- | --- | --- | --- | --- |
| `/api/auth/login` | `POST` | Không | `email`, `password` | `ApiResponseDTO<LoginResponse>` |
| `/api/auth/refresh-token` | `POST` | Không | `refreshToken` | `ApiResponseDTO<LoginResponse>` |
| `/api/auth/register` | `POST` | Không | `email`, `password`, `fullName`, `phone` | `ApiResponseDTO<RegisterResponse>` |
| `/api/auth/activate` | `GET` | Không | Query `token` | `ApiResponseDTO<RegisterResponse>` |
| `/api/auth/logout` | `POST` | Có | Header `Authorization` | `ApiResponseDTO<null>` |
| `/api/auth/health` | `GET` | Không | Không | `ApiResponseDTO<string>` |

Request mẫu login:

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

Response mẫu login:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "account": {
      "id": 1,
      "email": "user@example.com",
      "fullName": "Nguyen Van A",
      "phone": "0901234567",
      "status": "ACTIVE",
      "role": {
        "id": 1,
        "name": "ADMIN",
        "description": "Administrator"
      }
    }
  }
}
```

FE nên làm sau khi login thành công:

1. Lưu `accessToken`
2. Lưu `refreshToken`
3. Lưu `account`
4. Gắn `Authorization: Bearer <accessToken>` cho mọi private API

## 2.2 Public catalog

### What

API cho trang public: danh sách sản phẩm, chi tiết sản phẩm, categories, brands.

### Why

Đây là nhóm API chính để build homepage, listing page, product detail page, filter panel.

### When

- Load homepage / collection page
- Search sản phẩm
- Filter theo brand, category, giá, tồn kho
- Render product detail

### How

| Endpoint | Method | Auth | Query / Body | Trả về |
| --- | --- | --- | --- | --- |
| `/api/public/products` | `GET` | Không | `page`, `pageSize`, `q`, `sortBy`, `sortDirection`, `categoryId`, `brandId`, `minPrice`, `maxPrice`, `inStock` | `PageResponse<ProductListItemDTO>` |
| `/api/public/products/all` | `GET` | Không | Không | `ProductListItemDTO[]` |
| `/api/public/products/{id}` | `GET` | Không | Path `id` | `ProductDetailDTO` |
| `/api/public/products/search` | `GET` | Không | Giống `/products` nhưng `q` là bắt buộc | `PageResponse<ProductListItemDTO>` |
| `/api/public/products/search` | `POST` | Không | JSON filter body | `PageResponse<ProductListItemDTO>` |
| `/api/public/categories` | `GET` | Không | Không | `CategoryListItemDTO[]` |
| `/api/public/brands` | `GET` | Không | Không | `BrandListItemDTO[]` |

Query hay dùng:

```text
page=0
pageSize=20
q=ao so mi
categoryId=1
brandId=2
minPrice=100000
maxPrice=500000
inStock=true
sortBy=id
sortDirection=DESC
```

Response mẫu của `GET /api/public/products`:

```json
{
  "content": [
    {
      "id": 1,
      "name": "Ao so mi",
      "code": "SP001",
      "price": 250000,
      "status": "ACTIVE",
      "avatarImage": "/api/public/files/products/a.jpg",
      "brandName": "Sobu",
      "categoryName": "Ao",
      "stockAvailable": 12
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false,
  "hasNext": true,
  "hasPrevious": false
}
```

Field quan trọng của `ProductDetailDTO`:

- `id`
- `name`
- `code`
- `description`
- `content`
- `price`
- `oldPrice`
- `avatarImage`
- `brandName`
- `categoryName`
- `stockAvailable`
- `stockRemain`
- `units`
- `attributes`
- `images`
- `updatedAt`

## 2.3 Public UI config

### What

Banner và config public của website.

### Why

FE cần lấy dữ liệu trang chủ như banner, config text/color/image/json.

### When

- Load homepage
- Load global site settings
- Render banner theo device và vị trí

### How

| Endpoint | Method | Auth | Query / Params | Trả về |
| --- | --- | --- | --- | --- |
| `/api/public/ui/banners` | `GET` | Không | `deviceType`, `position` | `ApiResponseDTO<BannerDTO[]>` |
| `/api/public/ui/configs` | `GET` | Không | Không | `ApiResponseDTO<WebsiteConfigurationDTO[]>` |
| `/api/public/ui/configs/{key}` | `GET` | Không | Path `key` | `ApiResponseDTO<WebsiteConfigurationDTO>` |

Enum hợp lệ:

- `deviceType`: `WEB`, `MOBILE`, `ALL`
- `position`: `HOME_TOP`, `HOME_MIDDLE`, `PRODUCT_SIDEBAR`

## 2.4 File upload và file public

### What

Upload file lên server và lấy URL public của file đó.

### Why

FE thường cần upload ảnh trước, sau đó lấy `url` để nhét vào request/banner/config.

### When

- Upload ảnh request custom
- Upload ảnh banner
- Upload ảnh nội dung khác của admin

### How

| Endpoint | Method | Auth | Body | Trả về |
| --- | --- | --- | --- | --- |
| `/api/admin/files/upload` | `POST` | Có | `multipart/form-data` gồm `file`, optional `subDirectory` | `ApiResponseDTO<{ url: string }>` |
| `/api/public/files/**` | `GET` | Không | Path file | Trả binary image/file |

Ví dụ response upload:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "File uploaded successfully",
  "data": {
    "url": "/api/public/files/banners/abc.jpg"
  }
}
```

## 2.5 Customer request workflow

### What

API cho user tạo request mua hàng / preorder / finding / custom, xem danh sách request của mình, sửa request của mình.

### Why

Đây là flow quan trọng nếu FE có màn "gửi yêu cầu", "yêu cầu đặt hàng", "tracking yêu cầu".

### When

- User tạo request mới
- User vào trang "Yêu cầu của tôi"
- User mở chi tiết request
- User sửa request khi status còn cho phép

### How

| Endpoint | Method | Auth | Body / Query | Trả về |
| --- | --- | --- | --- | --- |
| `/api/requests` | `GET` | Có | `page`, `size`, `sortBy`, `sortDirection` | `ApiResponseDTO<PageResponse<RequestResponseDto>>` |
| `/api/requests/me` | `GET` | Có | `page`, `size`, `sortBy`, `sortDirection` | `ApiResponseDTO<PageResponse<RequestResponseDto>>` |
| `/api/requests/me/{requestId}` | `GET` | Có | Path `requestId` | `ApiResponseDTO<RequestResponseDto>` |
| `/api/requests` | `POST` | Có | `CreateRequestDto` | `ApiResponseDTO<RequestResponseDto>` |
| `/api/requests/{requestId}` | `PUT` | Có | `UpdateRequestDto` | `ApiResponseDTO<RequestResponseDto>` |

Enum `type` hợp lệ:

- `NORMAL`
- `PREORDER`
- `FINDING`
- `CUSTOM`

Request mẫu tạo request:

```json
{
  "customerPhone": "0901234567",
  "type": "CUSTOM",
  "customRequirements": "Tim hang mau den, chat lieu cotton",
  "uploadedImageUrls": [
    "/api/public/files/requests/img-1.jpg"
  ],
  "items": [
    {
      "nhanhProductId": "P001",
      "name": "Ao hoodie",
      "note": "Size L",
      "price": 350000,
      "quantity": 2,
      "metadataJson": "{\"color\":\"black\",\"size\":\"L\"}"
    }
  ]
}
```

Field quan trọng trong `RequestResponseDto`:

- `id`
- `requestCode`
- `customerPhone`
- `type`
- `status`
- `totalAmount`
- `depositAmount`
- `customRequirements`
- `nhanhOrderId`
- `nhanhOrderCode`
- `items`
- `attachments`
- `createdAt`
- `updatedAt`

Status request hiện có:

- `PENDING`
- `REVIEWING`
- `SOURCING`
- `WAITING_CUSTOMER`
- `APPROVED`
- `REJECTED`
- `CANCELLED`

Lưu ý rất quan trọng cho FE:

- Không phải status nào cũng sửa được.
- `APPROVED`, `REJECTED`, `CANCELLED` coi như gần như khóa sửa.
- Nếu update sai trạng thái, backend có thể trả `409 CONFLICT`.

## 2.6 Customer orders

### What

API xem chi tiết order của chính user.

### Why

Dùng cho trang tracking order sau khi request đã được convert thành order.

### When

- User mở trang chi tiết order bằng `orderId`
- Hoặc FE chỉ có `nhanhOrderId` / `nhanhOrderCode`

### How

| Endpoint | Method | Auth | Params | Trả về |
| --- | --- | --- | --- | --- |
| `/api/orders/me/{orderId}` | `GET` | Có | Path `orderId` | `ApiResponseDTO<OrderResponseDto>` |
| `/api/orders/me/by-nhanh/{nhanhOrderId}` | `GET` | Có | Path `nhanhOrderId` hoặc code | `ApiResponseDTO<OrderResponseDto>` |

Field hay dùng trong `OrderResponseDto`:

- `id`
- `orderCode`
- `requestId`
- `requestCode`
- `type`
- `status`
- `syncStatus`
- `totalAmount`
- `depositAmount`
- `customerName`
- `customerMobile`
- `customerAddress`
- `nhanhOrderId`
- `nhanhOrderCode`
- `syncError`
- `items`

Lưu ý:

- Hiện tại code `KHONG` có API list orders của customer.
- FE chỉ có API lấy detail order.

## 2.7 Admin request workflow

### What

API cho staff/admin xem toàn bộ request, sửa request, và đẩy request sang status mới.

### Why

Dùng cho trang quản trị request.

### When

- Staff mở danh sách request
- Staff mở chi tiết request
- Staff cập nhật thông tin request
- Staff bấm duyệt / từ chối / chuyển trạng thái

### How

| Endpoint | Method | Auth | Body / Query | Trả về |
| --- | --- | --- | --- | --- |
| `/api/admin/requests` | `GET` | Có, role `ADMIN` hoặc `STAFF` | `page`, `size`, `sortBy`, `sortDirection` | `ApiResponseDTO<PageResponse<RequestResponseDto>>` |
| `/api/admin/requests/{requestId}` | `GET` | Có, role `ADMIN` hoặc `STAFF` | Path `requestId` | `ApiResponseDTO<RequestResponseDto>` |
| `/api/admin/requests/{requestId}` | `PUT` | Có, role `ADMIN` hoặc `STAFF` | `UpdateRequestDto` | `ApiResponseDTO<RequestResponseDto>` |
| `/api/admin/requests/{requestId}/process` | `POST` | Có, role `ADMIN` hoặc `STAFF` | `ProcessRequestDto` | `ApiResponseDTO<RequestResponseDto>` |

Body mẫu `process`:

```json
{
  "targetStatus": "APPROVED",
  "note": "Da xac nhan voi khach"
}
```

Các chuyển trạng thái backend đang cho phép:

- Từ `PENDING` -> `REVIEWING`, `SOURCING`, `REJECTED`, `CANCELLED`
- Từ `REVIEWING` -> `SOURCING`, `WAITING_CUSTOMER`, `APPROVED`, `REJECTED`, `CANCELLED`
- Từ `SOURCING` -> `REVIEWING`, `WAITING_CUSTOMER`, `APPROVED`, `REJECTED`, `CANCELLED`
- Từ `WAITING_CUSTOMER` -> `REVIEWING`, `SOURCING`, `APPROVED`, `REJECTED`, `CANCELLED`
- Từ `APPROVED` -> `CANCELLED`

Nếu FE gửi sai transition, backend trả `409 CONFLICT`.

## 2.8 Admin orders

### What

API quản trị order nội bộ và retry sync sang Nhanh.

### Why

Dùng cho trang admin xem order đã convert từ request và xử lý order sync fail.

### When

- Admin mở danh sách orders
- Admin mở detail order
- Admin bấm retry nếu sync Nhanh bị lỗi

### How

| Endpoint | Method | Auth | Query / Params | Trả về |
| --- | --- | --- | --- | --- |
| `/api/admin/orders` | `GET` | Có, role `ADMIN` hoặc `STAFF` | `page`, `size`, `sortBy`, `sortDirection` | `ApiResponseDTO<PageResponse<OrderResponseDto>>` |
| `/api/admin/orders/{orderId}` | `GET` | Có, role `ADMIN` hoặc `STAFF` | Path `orderId` | `ApiResponseDTO<OrderResponseDto>` |
| `/api/admin/orders/{orderId}/sync/retry` | `POST` | Có, role `ADMIN` hoặc `STAFF` | Path `orderId` | `ApiResponseDTO<OrderSyncResultDto>` |

`OrderSyncResultDto` gồm:

- `orderId`
- `orderCode`
- `syncStatus`: `PENDING`, `SYNCED`, `FAILED`
- `nhanhOrderId`
- `nhanhOrderCode`
- `syncError`

## 2.9 Admin banners

### What

CRUD banner cho website.

### Why

FE admin cần tạo, sửa, xóa, tìm banner.

### When

- Tạo banner mới
- Sửa banner
- Mở detail banner
- Xóa banner
- Search danh sách banner

### How

| Endpoint | Method | Auth | Body | Trả về |
| --- | --- | --- | --- | --- |
| `/api/admin/banners` | `POST` | Có | `multipart/form-data` | `ApiResponseDTO<BannerDTO>` |
| `/api/admin/banners/{id}` | `PUT` | Có | `multipart/form-data` | `ApiResponseDTO<BannerDTO>` |
| `/api/admin/banners/{id}` | `GET` | Có | Path `id` | `ApiResponseDTO<BannerDTO>` |
| `/api/admin/banners/{id}` | `DELETE` | Có | Path `id` | `ApiResponseDTO<null>` |
| `/api/admin/banners/search` | `POST` | Có | `SearchRequest` | `ApiResponseDTO<PageResponse<BannerDTO>>` |

Format `multipart/form-data` cho create/update:

- Part `banner`: JSON object
- Part `image`: file image, optional

JSON `banner` mẫu:

```json
{
  "title": "Banner he",
  "imageUrl": "/api/public/files/banners/old.jpg",
  "linkUrl": "/collections/summer",
  "displayOrder": 1,
  "position": "HOME_TOP",
  "isActive": true,
  "startDate": "2026-05-26T00:00:00",
  "endDate": "2026-06-30T23:59:59",
  "deviceType": "WEB"
}
```

## 2.10 Admin website configs

### What

CRUD config hệ thống như text, color, image, json, number.

### Why

Dùng để FE/admin quản lý nội dung cấu hình website mà không phải sửa code.

### When

- Tạo config mới
- Sửa config
- Lấy config theo id
- Search config trong admin

### How

| Endpoint | Method | Auth | Body / Params | Trả về |
| --- | --- | --- | --- | --- |
| `/api/admin/configs` | `POST` | Có | `WebsiteConfigurationRequest` | `ApiResponseDTO<WebsiteConfigurationDTO>` |
| `/api/admin/configs/{id}` | `PUT` | Có | `WebsiteConfigurationRequest` | `ApiResponseDTO<WebsiteConfigurationDTO>` |
| `/api/admin/configs/{id}` | `GET` | Có | Path `id` | `ApiResponseDTO<WebsiteConfigurationDTO>` |
| `/api/admin/configs/{id}` | `DELETE` | Có | Path `id` | `ApiResponseDTO<null>` |
| `/api/admin/configs/search` | `POST` | Có | `SearchRequest` | `ApiResponseDTO<PageResponse<WebsiteConfigurationDTO>>` |

Body mẫu:

```json
{
  "key": "homepage.heroTitle",
  "value": "Summer Collection",
  "type": "text",
  "groupName": "homepage",
  "description": "Tieu de hero section",
  "isPublic": true
}
```

`type` hợp lệ:

- `text`
- `color`
- `image`
- `boolean_type`
- `json`
- `number`

## 2.11 Admin sync data

### What

Trigger sync dữ liệu từ Nhanh về local DB.

### Why

Dùng cho admin page hoặc nút thao tác manual sync.

### When

- Muốn đồng bộ lại products
- Muốn đồng bộ lại categories
- Muốn đồng bộ lại brands

### How

| Endpoint | Method | Auth | Body | Trả về |
| --- | --- | --- | --- | --- |
| `/api/admin/products/sync` | `POST` | Có | Không | `{ "message": "Sync success" }` |
| `/api/admin/categories/sync` | `POST` | Có | Không | `{ "message": "Sync success" }` |
| `/admin/brands/sync` | `POST` | Có | Không | `{ "message": "Sync success" }` |

Lưu ý:

- Route sync brand hiện tại là `/admin/brands/sync`, `KHONG` phải `/api/admin/brands/sync`.
- Đây là behavior đúng theo code hiện tại.

## 2.12 Nhanh integration

### What

2 endpoint phục vụ kết nối OAuth với Nhanh.

### Why

Thường chỉ admin/integration page dùng, không phải flow end-user.

### When

- Cần lấy login URL để bắt đầu OAuth
- Nhanh redirect callback về backend sau khi user authorize

### How

| Endpoint | Method | Auth | Params | Trả về |
| --- | --- | --- | --- | --- |
| `/api/nhanh/login` | `GET` | Không | Không | String URL để redirect user sang Nhanh |
| `/api/nhanh/oauth/callback` | `GET` | Không | Query `accessCode` | String `"Connected"` |

## 3. Search và pagination mẫu

Nhóm API admin list/search đang dùng format này:

```json
{
  "searchTerm": "banner",
  "page": 0,
  "pageSize": 20,
  "sortBy": "id",
  "sortDirection": "DESC"
}
```

Response page thường nằm trong `data`:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Retrieved successfully",
  "data": {
    "content": [],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

## 4. Luồng FE hay dùng nhất

### Public shopping flow

1. `GET /api/public/ui/banners`
2. `GET /api/public/categories`
3. `GET /api/public/brands`
4. `GET /api/public/products`
5. `GET /api/public/products/{id}`

### Login flow

1. `POST /api/auth/login`
2. Lưu `accessToken` + `refreshToken`
3. Gọi private API với Bearer token
4. Nếu token hết hạn: `POST /api/auth/refresh-token`

### Create request flow

1. `POST /api/admin/files/upload` nếu cần upload ảnh trước
2. `POST /api/requests`
3. `GET /api/requests/me`
4. `GET /api/requests/me/{requestId}`

### Admin request flow

1. `GET /api/admin/requests`
2. `GET /api/admin/requests/{requestId}`
3. `PUT /api/admin/requests/{requestId}`
4. `POST /api/admin/requests/{requestId}/process`

## 5. Những điểm FE cần nhớ

1. Không phải API nào cũng bọc trong `ApiResponseDTO`; nhóm public product trả raw object/list.
2. Request và order đều có nhiều alias route; nên cố định 1 convention trong FE để khỏi rối.
3. Hiện không có API list order cho customer.
4. Upload file trả về `url`, FE thường phải lấy URL này để gửi tiếp trong request khác.
5. Route sync brand đang khác convention chung: `/admin/brands/sync`.
6. Nếu muốn xem schema chi tiết hơn theo code hiện tại, mở Swagger UI ở `/swagger-ui/index.html`.
