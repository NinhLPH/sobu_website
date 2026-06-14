# API Specification

This document rewrites the Sobu backend API into a consistent endpoint-by-endpoint specification for frontend and admin integration.

## Common Conventions

### Base URLs

- Local API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Standard Success Response

Most authenticated and admin endpoints return this wrapper:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "OK",
  "data": {},
  "timestamp": "2026-05-26T10:00:00"
}
```

### Standard Error Response

```json
{
  "success": false,
  "statusCode": 400,
  "message": "Invalid request",
  "error": "BAD_REQUEST",
  "timestamp": "2026-05-26T10:00:00"
}
```

### Raw Response Exceptions

These public catalog endpoints return raw objects or raw page responses instead of `ApiResponseDTO`:

- `GET /api/public/products`
- `GET /api/public/products/all`
- `GET /api/public/products/search`
- `POST /api/public/products/search`
- `GET /api/public/products/{id}`
- `GET /api/public/categories`
- `GET /api/public/brands`
- `GET /api/public/files/**`
- Their `/api/v1/public/...` aliases

### Route Aliases

Some modules expose multiple base paths:

- Public catalog: `/api/public/...` and `/api/v1/public/...`
- Requests: `/api/requests/...`, `/api/request/...`, `/api/v1/requests/...`, `/api/v1/request/...`
- Orders: `/api/orders/...` and `/api/v1/orders/...`
- Admin requests: `/api/admin/requests/...` and `/api/v1/admin/requests/...`

This document uses the shortest canonical route in examples unless noted otherwise.

### Common Error Responses

#### 400 Bad Request

```json
{
  "success": false,
  "message": "Validation failed"
}
```

#### 401 Unauthorized

```json
{
  "success": false,
  "message": "Unauthorized"
}
```

#### 403 Forbidden

```json
{
  "success": false,
  "message": "Access denied"
}
```

#### 404 Not Found

```json
{
  "success": false,
  "message": "Resource not found"
}
```

#### 409 Conflict

```json
{
  "success": false,
  "message": "State transition is not allowed"
}
```

#### 500 Internal Server Error

```json
{
  "success": false,
  "message": "Internal server error"
}
```

## Endpoint

**Login**

### Method

`POST`

### URI

`/api/auth/login`

### Description

Authenticate a user and return access token, refresh token, and account profile.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| email | String | Yes | Account email |
| password | String | Yes | Account password |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
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
        "name": "ADMIN"
      }
    }
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `500`.

### Business Rules

* Store both `accessToken` and `refreshToken` after login.
* Private endpoints require `Authorization: Bearer <accessToken>`.

### Notes

* Token refresh should use `/api/auth/refresh-token`.

## Endpoint

**Refresh Token**

### Method

`POST`

### URI

`/api/auth/refresh-token`

### Description

Issue a new access token and refresh token from a valid refresh token.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "refreshToken": "eyJ..."
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| refreshToken | String | Yes | Previously issued refresh token |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Token refreshed",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `500`.

### Business Rules

* FE should call this endpoint when access token expires.

### Notes

* Returned payload shape matches login response.

## Endpoint

**Register**

### Method

`POST`

### URI

`/api/auth/register`

### Description

Register a new account.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "email": "user@example.com",
  "password": "123456",
  "fullName": "Nguyen Van A",
  "phone": "0901234567"
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| email | String | Yes | Account email |
| password | String | Yes | Account password |
| fullName | String | Yes | Full name |
| phone | String | Yes | Phone number |

### Success Response

#### HTTP Status

`201 Created`

```json
{
  "success": true,
  "statusCode": 201,
  "message": "Registration successful",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "phone": "0901234567",
    "role": {
      "id": 2,
      "name": "CUSTOMER",
      "description": "Customer account"
    },
    "status": "INACTIVE",
    "message": "Registration successful. Please check your email to activate your account."
  },
  "timestamp": "2026-06-11T10:00:00"
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `409`, `500`.

### Business Rules

* Email and phone should be unique.

### Notes

* Account activation may be required before login.

## Endpoint

**Resend Activation Email**

### Method

`POST`

### URI

`/api/auth/resend-activation`

### Description

Resend the activation email for an inactive account.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "email": "user@example.com"
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| email | String | Yes | Valid account email |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Activation email sent",
  "timestamp": "2026-06-11T10:00:00"
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `404`, `500`.

### Business Rules

* Requests are limited to once every 60 seconds.
* The account must exist and still require activation.

### Notes

* This endpoint is public.

## Endpoint

**Activate Account**

### Method

`GET`

### URI

`/api/auth/activate`

### Description

Activate an account using an activation token.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| token | String | Yes | Activation token |

Example:

```http
GET /api/auth/activate?token=abc123
```

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Account activated",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "phone": "0901234567",
    "role": {
      "id": 2,
      "name": "CUSTOMER",
      "description": "Customer account"
    },
    "status": "ACTIVE",
    "message": "Account activated"
  },
  "timestamp": "2026-06-11T10:00:00"
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `404`, `500`.

### Business Rules

* Activation token must be valid and not expired.

### Notes

* Usually called from an email link.

## Endpoint

**Logout**

### Method

`POST`

### URI

`/api/auth/logout`

### Description

Logout the current authenticated user.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

Example:

```http
Authorization: Bearer <access_token>
```

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

Optional request body:

```json
{
  "refreshToken": "eyJ..."
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| refreshToken | String | No | Refresh token to invalidate together with the access token |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `500`.

### Business Rules

* FE should clear local auth state after logout.

### Notes

* The access token is read from the `Authorization` header; the refresh token is read from the optional body.

## Endpoint

**Auth Health**

### Method

`GET`

### URI

`/api/auth/health`

### Description

Check authentication module health.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "OK",
  "data": "Auth service is running",
  "timestamp": "2026-06-11T10:00:00"
}
```

### Error Responses

Uses the common error responses. Typical statuses: `500`.

### Business Rules

* Intended for health checks or quick diagnostics.

### Notes

* Safe to call without authentication.

## Endpoint

**List Public Products**

### Method

`GET`

### URI

`/api/public/products`

### Description

Get paginated public product list for storefront pages.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| page | Integer | No | Page number, default `0` |
| pageSize | Integer | No | Page size, default `20` |
| q | String | No | Search keyword; mapped to `search` |
| search | String | No | Search keyword; `q` is also accepted |
| sortBy | String | No | Sort field, default `id` |
| sortDirection | String | No | `ASC` or `DESC`, default `DESC` |
| categoryId | Long | No | Filter by category |
| brandId | Long | No | Filter by brand |
| minPrice | Number | No | Minimum price |
| maxPrice | Number | No | Maximum price |
| inStock | Boolean | No | Filter available stock |

Example:

```http
GET /api/public/products?page=0&pageSize=20&categoryId=1&brandId=2&inStock=true
```

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "externalId": 10001,
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

### Error Responses

Typical statuses: `400`, `500`.

### Business Rules

* This endpoint returns a raw page response, not `ApiResponseDTO`.

### Notes

* Alias available at `/api/v1/public/products`.

## Endpoint

**List All Public Products**

### Method

`GET`

### URI

`/api/public/products/all`

### Description

Get all public products without pagination.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
[
  {
    "id": 1,
    "externalId": 10001,
    "name": "Ao so mi",
    "code": "SP001",
    "price": 250000,
    "status": "ACTIVE",
    "avatarImage": "/api/public/files/products/a.jpg",
    "brandName": "Sobu",
    "categoryName": "Ao",
    "stockAvailable": 12
  }
]
```

### Error Responses

Typical statuses: `500`.

### Business Rules

* Intended for lightweight catalog bootstrap or small datasets.

### Notes

* Alias available at `/api/v1/public/products/all`.

## Endpoint

**Get Public Product Detail**

### Method

`GET`

### URI

`/api/public/products/{id}`

### Description

Get public product detail by internal product ID.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| id | Long | Yes | Product ID |

Example:

```http
GET /api/public/products/123
```

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "id": 123,
  "externalId": 100123,
  "name": "Ao hoodie",
  "otherName": "Hoodie",
  "code": "SP123",
  "barcode": "8930000000123",
  "status": "ACTIVE",
  "description": "Cotton hoodie",
  "content": "Product content",
  "price": 350000,
  "wholesalePrice": 300000,
  "oldPrice": 390000,
  "vat": 10,
  "avatarImage": "/api/public/files/products/hoodie.jpg",
  "brandName": "Sobu",
  "categoryName": "Ao",
  "stockAvailable": 10,
  "stockRemain": 10,
  "units": [
    {
      "id": 1,
      "name": "Cai",
      "quantity": 1,
      "price": 350000
    }
  ],
  "attributes": [
    {
      "name": "color",
      "value": "black"
    }
  ],
  "images": [],
  "updatedAt": "2026-06-11T10:00:00"
}
```

### Error Responses

Typical statuses: `404`, `500`.

### Business Rules

* This endpoint returns a raw object, not `ApiResponseDTO`.

### Notes

* Alias available at `/api/v1/public/products/{id}`.

## Endpoint

**Search Public Products By Query**

### Method

`GET`

### URI

`/api/public/products/search`

### Description

Search public products using query parameters.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| q | String | Yes | Search keyword |
| page | Integer | No | Page number |
| pageSize | Integer | No | Page size |
| sortBy | String | No | Sort field |
| sortDirection | String | No | Sort direction |
| categoryId | Long | No | Filter by category |
| brandId | Long | No | Filter by brand |
| minPrice | Number | No | Minimum price |
| maxPrice | Number | No | Maximum price |
| inStock | Boolean | No | Filter available stock |

Example:

```http
GET /api/public/products/search?q=ao%20so%20mi&page=0&pageSize=20
```

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "content": [],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Error Responses

Typical statuses: `400`, `500`.

### Business Rules

* This endpoint returns a raw page response, not `ApiResponseDTO`.

### Notes

* Alias available at `/api/v1/public/products/search`.

## Endpoint

**Search Public Products By Filter Body**

### Method

`POST`

### URI

`/api/public/products/search`

### Description

Search public products using a JSON filter body.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "q": "ao hoodie",
  "page": 0,
  "pageSize": 20,
  "categoryId": 1,
  "brandId": 2,
  "minPrice": 100000,
  "maxPrice": 500000,
  "inStock": true,
  "sortBy": "id",
  "sortDirection": "DESC"
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| q | String | No | Search keyword |
| page | Integer | No | Page number |
| pageSize | Integer | No | Page size |
| categoryId | Long | No | Category filter |
| brandId | Long | No | Brand filter |
| minPrice | Number | No | Minimum price |
| maxPrice | Number | No | Maximum price |
| inStock | Boolean | No | Stock filter |
| sortBy | String | No | Sort field |
| sortDirection | String | No | Sort direction |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "content": [],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Error Responses

Typical statuses: `400`, `500`.

### Business Rules

* This endpoint returns a raw page response, not `ApiResponseDTO`.

### Notes

* Alias available at `/api/v1/public/products/search`.

## Endpoint

**List Public Categories**

### Method

`GET`

### URI

`/api/public/categories`

### Description

Get all public product categories.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
[
  {
    "id": 1,
    "parentId": null,
    "code": "AO",
    "name": "Ao",
    "order": 1,
    "image": "/api/public/files/categories/ao.jpg",
    "status": 1,
    "children": []
  }
]
```

### Error Responses

Typical statuses: `500`.

### Business Rules

* Intended for storefront filters and navigation.

### Notes

* Alias available at `/api/v1/public/categories`.

## Endpoint

**List Public Brands**

### Method

`GET`

### URI

`/api/public/brands`

### Description

Get all public product brands.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
[
  {
    "id": 1,
    "parentId": null,
    "code": "SOBU",
    "name": "Sobu",
    "status": 1
  }
]
```

### Error Responses

Typical statuses: `500`.

### Business Rules

* Intended for storefront filters and navigation.

### Notes

* Alias available at `/api/v1/public/brands`.

## Endpoint

**List Public Banners**

### Method

`GET`

### URI

`/api/public/ui/banners`

### Description

Get active public banners by device type and position.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| deviceType | String | No | `WEB`, `MOBILE`, `ALL` |
| position | String | No | `HOME_TOP`, `HOME_MIDDLE`, `PRODUCT_SIDEBAR` |

Example:

```http
GET /api/public/ui/banners?deviceType=WEB&position=HOME_TOP
```

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": [
    {
      "id": 1,
      "title": "Summer banner",
      "imageUrl": "/api/public/files/banners/banner-1.jpg",
      "position": "HOME_TOP",
      "deviceType": "WEB",
      "isActive": true
    }
  ]
}
```

### Error Responses

Uses the common error responses. Typical statuses: `500`.

### Business Rules

* Device type and position should match the supported enum values.

### Notes

* Returns `ApiResponseDTO<BannerDTO[]>`.

## Endpoint

**List Public Website Configs**

### Method

`GET`

### URI

`/api/public/ui/configs`

### Description

Get all public website configuration entries.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": [
    {
      "id": 1,
      "key": "homepage.heroTitle",
      "value": "Summer Collection",
      "type": "text",
      "groupName": "homepage",
      "isPublic": true
    }
  ]
}
```

### Error Responses

Uses the common error responses. Typical statuses: `500`.

### Business Rules

* Only public configs should be exposed here.

### Notes

* Returns `ApiResponseDTO<WebsiteConfigurationDTO[]>`.

## Endpoint

**Get Public Website Config By Key**

### Method

`GET`

### URI

`/api/public/ui/configs/{key}`

### Description

Get one public website configuration entry by key.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| key | String | Yes | Configuration key |

Example:

```http
GET /api/public/ui/configs/homepage.heroTitle
```

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "id": 1,
    "key": "homepage.heroTitle",
    "value": "Summer Collection",
    "type": "text"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `404`, `500`.

### Business Rules

* Key must be a public configuration key.

### Notes

* Returns `ApiResponseDTO<WebsiteConfigurationDTO>`.

## Endpoint

**Upload File**

### Method

`POST`

### URI

`/api/admin/files/upload`

### Description

Upload a file and receive a public URL.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `multipart/form-data` |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```text
multipart/form-data
- file: binary (required)
- subDirectory: string (optional)
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| file | File | Yes | File to upload |
| subDirectory | String | No | Optional folder name |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "url": "/api/public/files/banners/abc.jpg"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `500`.

### Business Rules

* FE should store the returned `url` and reuse it in later requests.

### Notes

* Public access is served from `/api/public/files/**`.

## Endpoint

**Get Public File**

### Method

`GET`

### URI

`/api/public/files/**`

### Description

Serve a public file or image from storage.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Response content type depends on file |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| filePath | String | Yes | Full file path after `/api/public/files/` |

Example:

```http
GET /api/public/files/banners/abc.jpg
```

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

Binary file content.

### Error Responses

Typical statuses: `404`, `500`.

### Business Rules

* URL returned by upload endpoints can be used directly here.

### Notes

* Used for product images, banners, request attachments, and other assets.

## Endpoint

**List Customer Requests**

### Method

`GET`

### URI

`/api/requests`

### Description

Get paginated request list for the authenticated user context.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| page | Integer | No | Page number |
| size | Integer | No | Page size |
| sortBy | String | No | Sort field |
| sortDirection | String | No | Sort direction |

Example:

```http
GET /api/requests?page=0&size=20&sortBy=id&sortDirection=DESC
```

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "content": [],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `500`.

### Business Rules

* Request endpoints also support `/api/request` and `/api/v1/...` aliases.

### Notes

* Prefer one alias convention in FE to avoid route confusion.

## Endpoint

**List My Requests**

### Method

`GET`

### URI

`/api/requests/me`

### Description

Get paginated requests belonging to the authenticated user.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| page | Integer | No | Page number |
| size | Integer | No | Page size |
| sortBy | String | No | Sort field |
| sortDirection | String | No | Sort direction |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "content": []
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `500`.

### Business Rules

* Use this endpoint for "My Requests" pages.

### Notes

* Returns `ApiResponseDTO<PageResponse<RequestResponseDto>>`.

## Endpoint

**Get My Request Detail**

### Method

`GET`

### URI

`/api/requests/me/{requestId}`

### Description

Get request detail for the authenticated user's own request.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| requestId | Long | Yes | Request ID |

Example:

```http
GET /api/requests/me/123
```

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "id": 123,
    "requestCode": "REQ-001",
    "customerPhone": "0901234567",
    "type": "CUSTOM",
    "status": "PENDING",
    "items": [],
    "attachments": []
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `404`, `500`.

### Business Rules

* Only owner-visible requests should be returned.

### Notes

* Returns `ApiResponseDTO<RequestResponseDto>`.

## Endpoint

**Create Request**

### Method

`POST`

### URI

`/api/requests`

### Description

Create a new customer request such as normal, preorder, finding, or custom.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |
| Authorization | String | Yes | Bearer token |
| Idempotency-Key | String | No | Makes retries return the original create-request response; conflicting payloads return `409` |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

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

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| customerPhone | String | Yes | Customer phone |
| type | String | Yes | `NORMAL`, `PREORDER`, `FINDING`, `CUSTOM` |
| customRequirements | String | No | Custom request note |
| uploadedImageUrls | Array<String> | No | Uploaded image URLs |
| items | Array<Object> | Yes | Requested item list |

### Success Response

#### HTTP Status

`201 Created`

```json
{
  "success": true,
  "statusCode": 201,
  "message": "Request created",
  "data": {
    "id": 123,
    "requestCode": "REQ-001",
    "customerPhone": "0901234567",
    "type": "CUSTOM",
    "status": "PENDING",
    "totalAmount": 700000,
    "depositAmount": 0,
    "customRequirements": "Tim hang mau den, chat lieu cotton",
    "nhanhOrderId": null,
    "nhanhOrderCode": null,
    "items": [],
    "attachments": [],
    "createdAt": "2026-06-11T10:00:00",
    "updatedAt": "2026-06-11T10:00:00"
  },
  "timestamp": "2026-06-11T10:00:00"
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `500`.

### Business Rules

* Uploaded image URLs should come from the file upload endpoint.
* Request type must be one of the supported enums.

### Notes

* Returns `ApiResponseDTO<RequestResponseDto>`.

## Endpoint

**Update Request**

### Method

`PUT`

### URI

`/api/requests/{requestId}`

### Description

Update an existing customer request when its status still allows editing.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| requestId | Long | Yes | Request ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "customerPhone": "0901234567",
  "type": "CUSTOM",
  "customRequirements": "Cap nhat ghi chu",
  "totalAmount": 700000,
  "depositAmount": 200000,
  "uploadedImageUrls": [],
  "items": []
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| customerPhone | String | No | Customer phone |
| type | String | No | `NORMAL`, `PREORDER`, `FINDING`, `CUSTOM` |
| customRequirements | String | No | Updated custom note |
| totalAmount | Number | No | Updated total amount, must be greater than or equal to `0` |
| depositAmount | Number | No | Updated deposit amount, must be greater than or equal to `0` |
| uploadedImageUrls | Array<String> | No | Replacement uploaded image URL list |
| items | Array<Object> | No | Updated item list |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Updated successfully",
  "data": {
    "id": 123,
    "status": "REVIEWING"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `404`, `409`, `500`.

### Business Rules

* Requests in `APPROVED`, `REJECTED`, or `CANCELLED` should be treated as locked.

### Notes

* Backend may return `409 CONFLICT` for invalid status-based edits.

## Endpoint

**Create Normal Order**

### Method

`POST`

### URI

`/api/orders`

### Description

Create a normal catalog order for authenticated customers.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |
| Authorization | String | Yes | Bearer token |
| Idempotency-Key | String | No | Makes retries return the original create-order response; conflicting payloads return `409` |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "customerName": "Nguyen Van A",
  "customerMobile": "0901234567",
  "customerEmail": "user@example.com",
  "customerAddress": "Ha Noi",
  "customerCityName": "Ha Noi",
  "customerDistrictName": "Cau Giay",
  "customerWardName": "Dich Vong",
  "customerCityId": 1,
  "customerDistrictId": 10,
  "customerWardId": 100,
  "carrierId": 5,
  "carrierServiceId": 2,
  "shippingFee": 30000,
  "description": "Giao gio hanh chinh",
  "items": [
    {
      "nhanhProductId": "10001",
      "name": "Ao hoodie",
      "note": "Size L",
      "price": 350000,
      "discount": 0,
      "quantity": 2
    }
  ]
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| customerName | String | Yes | Customer name |
| customerMobile | String | Yes | Customer phone |
| customerEmail | String | No | Customer email |
| customerAddress | String | No | Delivery address |
| customerCityName | String | No | City name |
| customerDistrictName | String | No | District name |
| customerWardName | String | No | Ward name |
| customerCityId | Long | No | Nhanh city/province ID |
| customerDistrictId | Long | No | Nhanh district ID |
| customerWardId | Long | No | Nhanh ward ID |
| carrierId | Long | No | Carrier ID |
| carrierServiceId | Long | No | Carrier service ID |
| shippingFee | Number | No | Shipping fee |
| description | String | No | Order note |
| items | Array<Object> | Yes | Order items containing required `nhanhProductId`, `name`, `price`, `quantity`; optional `note`, `discount` |

### Success Response

#### HTTP Status

`201 Created`

```json
{
  "success": true,
  "message": "Order created",
  "data": {
    "id": 1,
    "orderCode": "ORD-001",
    "requestId": null,
    "requestCode": null,
    "type": "NORMAL",
    "status": "NEW",
    "syncStatus": "PENDING",
    "nhanhSyncStage": "NONE",
    "totalAmount": 730000,
    "depositAmount": 0,
    "shippingFee": 30000,
    "paidAmount": 0,
    "remainingAmount": 730000,
    "paymentStatus": "PENDING",
    "items": []
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `500`.

### Business Rules

* At least one item is required.
* Nhanh sync depends on order payment milestone rules.

### Notes

* Alias available at `/api/v1/orders`.

## Endpoint

**Get My Order Detail**

### Method

`GET`

### URI

`/api/orders/me/{orderId}`

### Description

Get authenticated customer's order detail by internal order ID.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| orderId | Long | Yes | Internal order ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Order retrieved",
  "data": {
    "id": 1,
    "orderCode": "ORD-001",
    "requestId": 10,
    "status": "PENDING",
    "syncStatus": "PENDING",
    "totalAmount": 700000,
    "depositAmount": 0,
    "nhanhOrderId": null,
    "items": []
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `404`, `500`.

### Business Rules

* Order visibility is bound to the authenticated account phone.

### Notes

* Alias available at `/api/v1/orders/me/{orderId}`.

## Endpoint

**Get My Order By Nhanh ID**

### Method

`GET`

### URI

`/api/orders/me/by-nhanh/{nhanhOrderId}`

### Description

Get authenticated customer's order detail by Nhanh order ID or Nhanh order code.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| nhanhOrderId | String | Yes | Nhanh order ID or code |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Order retrieved",
  "data": {
    "id": 1,
    "nhanhOrderId": "123456",
    "nhanhOrderCode": "NH001"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `404`, `500`.

### Business Rules

* Use when FE only knows external Nhanh reference.

### Notes

* Alias available at `/api/v1/orders/me/by-nhanh/{nhanhOrderId}`.

## Endpoint

**List Order Payments**

### Method

`GET`

### URI

`/api/orders/{orderId}/payments`

### Description

List payment records for the authenticated customer's order.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| orderId | Long | Yes | Internal order ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Payments retrieved",
  "data": [
    {
      "id": 1,
      "orderId": 10,
      "paymentCode": "PAY-001",
      "type": "DEPOSIT",
      "paymentMethod": "ONLINE",
      "status": "PENDING",
      "amount": 300000,
      "provider": "PAYOS",
      "providerReference": null,
      "providerOrderCode": 100001,
      "checkoutUrl": "https://pay.payos.vn/web/...",
      "qrCode": "000201...",
      "failureReason": null,
      "expiresAt": "2026-06-11T10:15:00",
      "paidAt": null,
      "createdAt": "2026-06-11T10:00:00",
      "updatedAt": "2026-06-11T10:00:00"
    }
  ]
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `404`, `500`.

### Business Rules

* Order must belong to the authenticated customer.

### Notes

* Alias available at `/api/v1/orders/{orderId}/payments`.

## Endpoint

**Create Order Payment**

### Method

`POST`

### URI

`/api/orders/{orderId}/payments`

### Description

Create a payment checkout session for the authenticated customer's order.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |
| Authorization | String | Yes | Bearer token |
| Idempotency-Key | String | No | Makes retries return the original create-payment response; conflicting payloads return `409` |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| orderId | Long | Yes | Internal order ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "type": "DEPOSIT",
  "paymentMethod": "ONLINE"
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| type | String | Yes | `FULL`, `DEPOSIT`, `FINAL`, `REFUND` |
| paymentMethod | String | Yes | `ONLINE` or `COD` |

### Success Response

#### HTTP Status

`201 Created`

```json
{
  "success": true,
  "statusCode": 201,
  "message": "Payment created",
  "data": {
    "id": 1,
    "orderId": 10,
    "paymentCode": "PAY-001",
    "type": "DEPOSIT",
    "paymentMethod": "ONLINE",
    "status": "PENDING",
    "amount": 300000,
    "provider": "PAYOS",
    "providerReference": null,
    "providerOrderCode": 100001,
    "checkoutUrl": "https://pay.payos.vn/web/...",
    "qrCode": "000201...",
    "failureReason": null,
    "expiresAt": "2026-06-11T10:15:00",
    "paidAt": null,
    "createdAt": "2026-06-11T10:00:00",
    "updatedAt": "2026-06-11T10:00:00"
  },
  "timestamp": "2026-06-11T10:00:00"
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `404`, `409`, `500`.

### Business Rules

* Payment creation is allowed only in eligible order phases.

### Notes

* Alias available at `/api/v1/orders/{orderId}/payments`.

## Endpoint

**List Admin Requests**

### Method

`GET`

### URI

`/api/admin/requests`

### Description

Get paginated request list for admin or staff users.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| page | Integer | No | Page number |
| size | Integer | No | Page size |
| sortBy | String | No | Sort field |
| sortDirection | String | No | Sort direction |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Orders retrieved",
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

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `500`.

### Business Rules

* Requires role `ADMIN` or `STAFF`.

### Notes

* Alias available at `/api/v1/admin/requests`.

## Endpoint

**Get Admin Request Detail**

### Method

`GET`

### URI

`/api/admin/requests/{requestId}`

### Description

Get request detail for admin or staff users.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| requestId | Long | Yes | Request ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "id": 123,
    "requestCode": "REQ-001",
    "status": "REVIEWING"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `404`, `500`.

### Business Rules

* Requires role `ADMIN` or `STAFF`.

### Notes

* Returns `ApiResponseDTO<RequestResponseDto>`.

## Endpoint

**Update Admin Request**

### Method

`PUT`

### URI

`/api/admin/requests/{requestId}`

### Description

Update request data as admin or staff.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| requestId | Long | Yes | Request ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "customerPhone": "0901234567",
  "customRequirements": "Da cap nhat",
  "items": []
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| customerPhone | String | No | Customer phone |
| customRequirements | String | No | Updated note |
| items | Array<Object> | No | Updated item list |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Updated successfully",
  "data": {
    "id": 123,
    "status": "SOURCING"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `403`, `404`, `409`, `500`.

### Business Rules

* Requires role `ADMIN` or `STAFF`.

### Notes

* Returns `ApiResponseDTO<RequestResponseDto>`.

## Endpoint

**Process Admin Request Status**

### Method

`POST`

### URI

`/api/admin/requests/{requestId}/process`

### Description

Move a request to a new business status as admin or staff.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| requestId | Long | Yes | Request ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "targetStatus": "APPROVED",
  "note": "Da xac nhan voi khach",
  "depositAmount": 300000
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| targetStatus | String | Yes | Target request status |
| note | String | No | Internal processing note |
| depositAmount | Number | No | Optional deposit override applied before an approved request converts to an order |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Request processed",
  "data": {
    "id": 123,
    "status": "APPROVED"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `403`, `404`, `409`, `500`.

### Business Rules

* Allowed transitions:
* `PENDING -> REVIEWING, SOURCING, REJECTED, CANCELLED`
* `REVIEWING -> SOURCING, WAITING_CUSTOMER, APPROVED, REJECTED, CANCELLED`
* `SOURCING -> REVIEWING, WAITING_CUSTOMER, APPROVED, REJECTED, CANCELLED`
* `WAITING_CUSTOMER -> REVIEWING, SOURCING, APPROVED, REJECTED, CANCELLED`
* `APPROVED -> CANCELLED`

### Notes

* Invalid transitions return `409 CONFLICT`.

## Endpoint

**List Admin Orders**

### Method

`GET`

### URI

`/api/admin/orders`

### Description

Get paginated order list for admin or staff users.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| page | Integer | No | Page number |
| size | Integer | No | Page size |
| sortBy | String | No | Sort field |
| sortDirection | String | No | Sort direction |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "content": []
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `500`.

### Business Rules

* Requires role `ADMIN` or `STAFF`.

### Notes

* Returns `ApiResponseDTO<PageResponse<OrderResponseDto>>`.

## Endpoint

**Get Admin Order Detail**

### Method

`GET`

### URI

`/api/admin/orders/{orderId}`

### Description

Get order detail for admin or staff users.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| orderId | Long | Yes | Order ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "id": 1,
    "orderCode": "ORD-001",
    "syncStatus": "FAILED",
    "syncError": "Nhanh timeout"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `404`, `500`.

### Business Rules

* Requires role `ADMIN` or `STAFF`.

### Notes

* Useful for failed sync investigation.

## Endpoint

**Retry Admin Order Sync**

### Method

`POST`

### URI

`/api/admin/orders/{orderId}/sync/retry`

### Description

Retry syncing an order to Nhanh.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| orderId | Long | Yes | Order ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Order sync retry completed",
  "data": {
    "orderId": 1,
    "orderCode": "ORD-001",
    "syncStatus": "SYNCED",
    "nhanhSyncStage": "NORMAL_ORDER_CREATED",
    "nhanhOrderId": "123456",
    "nhanhOrderCode": "NH001",
    "syncError": null,
    "lastSyncMessage": "Order synchronized",
    "lastSyncAt": "2026-06-11T10:00:00"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `404`, `500`.

### Business Rules

* Requires role `ADMIN` or `STAFF`.

### Notes

* Returns `ApiResponseDTO<OrderSyncResultDto>`.

## Endpoint

**Confirm Mock Payment**

### Method

`POST`

### URI

`/v1/api/admin/payments/{paymentCode}/mock/confirm`

### Description

Confirm a mock payment and trigger post-payment order sync when eligible.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| paymentCode | String | Yes | Payment code |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Payment confirmed",
  "data": {
    "paymentCode": "PAY-001",
    "status": "PAID"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `404`, `500`.

### Business Rules

* Requires role `ADMIN` or `STAFF`.

### Notes

* This route follows `/v1/api/...`, not `/api/v1/...`.

## Endpoint

**Create Preorder Final Payment**

### Method

`POST`

### URI

`/v1/api/admin/payments/orders/{orderId}/final`

### Description

Create the final mock payment session for an eligible preorder.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| orderId | Long | Yes | Order ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`201 Created`

```json
{
  "success": true,
  "message": "Final payment created",
  "data": {
    "paymentCode": "PAY-002",
    "type": "FINAL",
    "status": "PENDING"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `404`, `409`, `500`.

### Business Rules

* Requires role `ADMIN` or `STAFF`.
* Only eligible preorders can move to final payment.

### Notes

* This route follows `/v1/api/...`, not `/api/v1/...`.

## Endpoint

**Create Banner**

### Method

`POST`

### URI

`/api/admin/banners`

### Description

Create a banner with optional image upload.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `multipart/form-data` |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```text
multipart/form-data
- banner: JSON object (required)
- image: binary file (optional)
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| banner | JSON | Yes | Banner metadata |
| image | File | No | New banner image |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Created successfully",
  "data": {
    "id": 1,
    "title": "Banner he"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `403`, `500`.

### Business Rules

* Banner metadata should include valid position and device type.

### Notes

* `banner` JSON often contains `title`, `imageUrl`, `linkUrl`, `displayOrder`, `position`, `isActive`, `startDate`, `endDate`, `deviceType`.

## Endpoint

**Update Banner**

### Method

`PUT`

### URI

`/api/admin/banners/{id}`

### Description

Update an existing banner with optional image replacement.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `multipart/form-data` |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| id | Long | Yes | Banner ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```text
multipart/form-data
- banner: JSON object (required)
- image: binary file (optional)
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| banner | JSON | Yes | Updated banner metadata |
| image | File | No | Replacement image |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Updated successfully",
  "data": {
    "id": 1,
    "title": "Banner moi"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `403`, `404`, `500`.

### Business Rules

* Banner ID must exist.

### Notes

* Same multipart contract as create.

## Endpoint

**Get Banner Detail**

### Method

`GET`

### URI

`/api/admin/banners/{id}`

### Description

Get banner detail by ID.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| id | Long | Yes | Banner ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "id": 1,
    "title": "Banner he"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `404`, `500`.

### Business Rules

* Banner ID must exist.

### Notes

* Returns `ApiResponseDTO<BannerDTO>`.

## Endpoint

**Delete Banner**

### Method

`DELETE`

### URI

`/api/admin/banners/{id}`

### Description

Delete a banner by ID.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| id | Long | Yes | Banner ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Deleted successfully",
  "data": null
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `404`, `500`.

### Business Rules

* Banner ID must exist.

### Notes

* Clients should remove deleted banner from local cache.

## Endpoint

**Search Banners**

### Method

`POST`

### URI

`/api/admin/banners/search`

### Description

Search banners with pagination and sort options.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "searchTerm": "banner",
  "page": 0,
  "pageSize": 20,
  "sortBy": "id",
  "sortDirection": "DESC"
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| searchTerm | String | Yes | Search keyword |
| page | Integer | No | Page number, default `0` |
| pageSize | Integer | No | Page size, default `20`, max `100` |
| sortBy | String | No | Sort field, default `id` |
| sortDirection | String | No | `ASC` or `DESC`, default `DESC` |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "content": []
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `403`, `500`.

### Business Rules

* `searchTerm` is required.

### Notes

* Uses the shared `SearchRequest` contract.

## Endpoint

**Create Website Config**

### Method

`POST`

### URI

`/api/admin/configs`

### Description

Create a website configuration entry.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

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

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| key | String | Yes | Config key |
| value | String | Yes | Config value |
| type | String | Yes | `text`, `color`, `image`, `boolean_type`, `json`, `number` |
| groupName | String | No | Config group |
| description | String | No | Config description |
| isPublic | Boolean | No | Public visibility flag |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Created successfully",
  "data": {
    "id": 1,
    "key": "homepage.heroTitle"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `403`, `409`, `500`.

### Business Rules

* `key` should be unique within website config records.

### Notes

* Returns `ApiResponseDTO<WebsiteConfigurationDTO>`.

## Endpoint

**Update Website Config**

### Method

`PUT`

### URI

`/api/admin/configs/{id}`

### Description

Update a website configuration entry.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| id | Long | Yes | Config ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "key": "homepage.heroTitle",
  "value": "New Hero",
  "type": "text",
  "groupName": "homepage",
  "description": "Updated title",
  "isPublic": true
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| key | String | Yes | Config key |
| value | String | Yes | Config value |
| type | String | Yes | Config type |
| groupName | String | No | Config group |
| description | String | No | Config description |
| isPublic | Boolean | No | Public visibility flag |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Updated successfully",
  "data": {
    "id": 1,
    "key": "homepage.heroTitle"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `403`, `404`, `500`.

### Business Rules

* Config ID must exist.

### Notes

* Returns `ApiResponseDTO<WebsiteConfigurationDTO>`.

## Endpoint

**Get Website Config Detail**

### Method

`GET`

### URI

`/api/admin/configs/{id}`

### Description

Get website configuration detail by ID.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| id | Long | Yes | Config ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "id": 1,
    "key": "homepage.heroTitle",
    "value": "Summer Collection"
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `404`, `500`.

### Business Rules

* Config ID must exist.

### Notes

* Returns `ApiResponseDTO<WebsiteConfigurationDTO>`.

## Endpoint

**Delete Website Config**

### Method

`DELETE`

### URI

`/api/admin/configs/{id}`

### Description

Delete a website configuration entry.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| id | Long | Yes | Config ID |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Deleted successfully",
  "data": null
}
```

### Error Responses

Uses the common error responses. Typical statuses: `401`, `403`, `404`, `500`.

### Business Rules

* Config ID must exist.

### Notes

* Remove deleted entry from FE cache if applicable.

## Endpoint

**Search Website Configs**

### Method

`POST`

### URI

`/api/admin/configs/search`

### Description

Search website configuration entries with pagination and sorting.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | Yes | `application/json` |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

```json
{
  "searchTerm": "homepage",
  "page": 0,
  "pageSize": 20,
  "sortBy": "id",
  "sortDirection": "DESC"
}
```

#### Body Fields

| Field | Type | Required | Description |
| ----- | ---- | -------- | ----------- |
| searchTerm | String | Yes | Search keyword |
| page | Integer | No | Page number |
| pageSize | Integer | No | Page size |
| sortBy | String | No | Sort field |
| sortDirection | String | No | Sort direction |

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "success": true,
  "message": "Retrieved successfully",
  "data": {
    "content": []
  }
}
```

### Error Responses

Uses the common error responses. Typical statuses: `400`, `401`, `403`, `500`.

### Business Rules

* `searchTerm` is required.

### Notes

* Uses the shared `SearchRequest` contract.

## Endpoint

**Sync Products**

### Method

`POST`

### URI

`/api/admin/products/sync`

### Description

Trigger manual product sync from Nhanh into local database.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "message": "Sync success"
}
```

### Error Responses

Typical statuses: `401`, `403`, `500`.

### Business Rules

* Intended for admin or staff use.

### Notes

* Response is a simple object, not the standard wrapper.

## Endpoint

**Sync Categories**

### Method

`POST`

### URI

`/api/admin/categories/sync`

### Description

Trigger manual category sync from Nhanh into local database.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "message": "Sync success"
}
```

### Error Responses

Typical statuses: `401`, `403`, `500`.

### Business Rules

* Intended for admin or staff use.

### Notes

* Response is a simple object, not the standard wrapper.

## Endpoint

**Sync Brands**

### Method

`POST`

### URI

`/api/admin/brands/sync`

### Description

Trigger manual brand sync from Nhanh into local database.

### Authorization

| Type | Required |
| ---- | -------- |
| Bearer Token | Yes |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Authorization | String | Yes | Bearer token |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
{
  "message": "Sync success"
}
```

### Error Responses

Typical statuses: `401`, `403`, `500`.

### Business Rules

* Intended for admin or staff use.

### Notes

* Current code uses `/api/admin/brands/sync`.

## Endpoint

**Get Nhanh Login URL**

### Method

`GET`

### URI

`/api/nhanh/login`

### Description

Get the Nhanh OAuth login URL.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
"https://nhanh.vn/oauth?version=3.0&appId=...&returnLink=..."
```

### Error Responses

Typical statuses: `500`.

### Business Rules

* Used to start the OAuth flow with Nhanh.

### Notes

* Intended for integration/admin tooling, not end-user shopping flow.

## Endpoint

**Receive Nhanh Webhook**

### Method

`POST`

### URI

`/api/nhanh/webhooks`

### Description

Receive and record a webhook callback from Nhanh.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Depends on the callback sent by Nhanh |
| Any webhook headers | String | No | All request headers are forwarded to the webhook service |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

Raw request body. The body may be absent.

Example:

```json
{
  "event": "order.update",
  "data": {}
}
```

### Success Response

#### HTTP Status

`200 OK`

```text
OK
```

### Error Responses

The controller is designed to acknowledge recorded callbacks with `200`. Unexpected unhandled failures may return `500`.

### Business Rules

* The raw body and headers are passed to `NhanhWebhookService`.
* The endpoint is public so Nhanh can call it without a JWT.

### Notes

* Equivalent aliases: `/api/nhanh/webhooks/` and `/api/nhanh/webhooks/callback`.
* Response is raw text, not `ApiResponseDTO`.

## Endpoint

**Receive PayOS Webhook**

### Method

`POST`

### URI

`/api/payos/webhooks`

### Description

Receive a PayOS payment callback, record the raw payload, and apply payment confirmation when applicable.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Usually `application/json` |
| PayOS webhook headers | String | No | All request headers are forwarded to the webhook service |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Request Body

Raw PayOS callback body. The body may be absent.

Example:

```json
{
  "code": "00",
  "success": true,
  "data": {}
}
```

### Success Response

#### HTTP Status

`200 OK`

```text
OK
```

### Error Responses

Unexpected unhandled failures may return `500`.

### Business Rules

* Payment status is updated only when the callback can be matched and validated by `PayOSWebhookService`.
* The endpoint is public so PayOS can call it without a JWT.

### Notes

* Equivalent aliases: `/api/payos/webhooks/` and `/api/payos/webhooks/callback`.
* Response is raw text, not `ApiResponseDTO`.

## Endpoint

**Cancel PayOS Payment**

### Method

`GET`

### URI

`/api/payos/payments/cancel`

### Description

Mark a payment as failed after checkout cancellation and redirect the browser.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| None | - | - | - |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| paymentCode | String | Yes | Internal payment code to mark as failed |
| redirect | String | No | Redirect target; defaults to `/` |

Example:

```http
GET /api/payos/payments/cancel?paymentCode=PAY-001&redirect=https%3A%2F%2Fshop.example.com%2Fpayment-result
```

### Request Body

No request body.

### Success Response

#### HTTP Status

`302 Found`

```http
Location: https://shop.example.com/payment-result?paymentCode=PAY-001&status=FAILED
```

### Error Responses

Typical statuses: `400`, `404`, `500`.

### Business Rules

* The payment is marked failed with reason `Payment cancelled by customer`.
* The redirect URL receives `paymentCode` and `status=FAILED` query parameters.

### Notes

* Response has no JSON body.

## Endpoint

**Handle Nhanh OAuth Callback**

### Method

`GET`

### URI

`/api/nhanh/oauth/callback`

### Description

Handle OAuth callback from Nhanh after authorization.

### Authorization

| Type | Required |
| ---- | -------- |
| None | No |

### Headers

| Header | Type | Required | Description |
| ------ | ---- | -------- | ----------- |
| Content-Type | String | No | Optional for GET requests |

### Path Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| None | - | - | - |

### Query Parameters

| Parameter | Type | Required | Description |
| --------- | ---- | -------- | ----------- |
| accessCode | String | Yes | Nhanh OAuth access code |

Example:

```http
GET /api/nhanh/oauth/callback?accessCode=abc123
```

### Request Body

No request body.

### Success Response

#### HTTP Status

`200 OK`

```json
"Connected"
```

### Error Responses

Typical statuses: `400`, `500`.

### Business Rules

* Callback should be invoked by Nhanh after user approval.

### Notes

* Intended for integration/admin tooling, not end-user shopping flow.
