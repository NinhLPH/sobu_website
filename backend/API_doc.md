# Soduo API Documentation

This document provides a comprehensive overview of the APIs available in the Soduo Backend application.

## 1. General Information

### Base URL
All API endpoints are prefixed with the base URL of the application.
- Development: `http://localhost:8080`
- Production: `https://api.sodu.vn` (Example)

### Authentication
Most `/api/admin/**` endpoints require authentication using a JWT (JSON Web Token).
To authenticate, include the token in the `Authorization` header of your request:
```http
Authorization: Bearer <your_jwt_token>
```

### Standard Response Format
All APIs return a standard JSON response structure defined by `ApiResponseDTO`:

```json
{
  "success": true,
  "statusCode": 200,
  "message": "Operation successful",
  "data": { ... },
  "error": null,
  "timestamp": "2024-05-20T10:00:00"
}
```

---

## 2. Authentication API (`/api/auth`)

Endpoints for user authentication, registration, and token management.

### User Login
Authenticates a user and returns an access token and refresh token.
- **URL**: `/api/auth/login`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "yourpassword"
  }
  ```
- **Response**: `ApiResponseDTO<LoginResponse>`

### User Registration
Registers a new user account. An activation email will be sent.
- **URL**: `/api/auth/register`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "yourpassword",
    "fullName": "John Doe",
    "phone": "0123456789"
  }
  ```
- **Response**: `ApiResponseDTO<RegisterResponse>`

### Refresh Token
Generates a new access token using a refresh token.
- **URL**: `/api/auth/refresh-token`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "refreshToken": "your-refresh-token"
  }
  ```

### Activate Account
Activates a user account using a token received via email.
- **URL**: `/api/auth/activate`
- **Method**: `GET`
- **Query Params**: `token` (String)

### Logout
Invalidates the current session.
- **URL**: `/api/auth/logout`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <token>`

### Health Check
Check if the auth service is running.
- **URL**: `/api/auth/health`
- **Method**: `GET`

---

## 3. Storage API (`/api/storage`)

Endpoints for file management.

### Upload File (Admin)
Uploads a file to the server.
- **URL**: `/api/admin/files/upload`
- **Method**: `POST`
- **Request Body**: `MultipartForm`
    - `file`: The file to upload.
    - `subDirectory` (Optional): Target subdirectory.
- **Response**: Returns the URL of the uploaded file.

### View File (Public)
Retrieves a file by its path.
- **URL**: `/api/public/files/{filepath}`
- **Method**: `GET`

---

## 4. Website Configuration API (Admin)

Manage system-wide configurations.

### Create Configuration
- **URL**: `/api/admin/configs`
- **Method**: `POST`
- **Request Body**: `WebsiteConfigurationRequest`

### Update Configuration
- **URL**: `/api/admin/configs/{id}`
- **Method**: `PUT`

### Search Configurations
- **URL**: `/api/admin/configs/search`
- **Method**: `POST`
- **Request Body**: `SearchRequest`
  ```json
  {
    "searchTerm": "theme",
    "page": 0,
    "pageSize": 20,
    "sortBy": "key",
    "sortDirection": "ASC"
  }
  ```

---

## 5. Banner Management API

### Public: Get Active Banners
- **URL**: `/api/public/ui/banners`
- **Method**: `GET`
- **Query Params**: 
    - `deviceType` (Optional): `WEB`, `MOBILE`, `ALL`
    - `position` (Optional): `HOME_TOP`, `HOME_MIDDLE`, `PRODUCT_SIDEBAR`

### Admin: Create Banner
- **URL**: `/api/admin/banners`
- **Method**: `POST`
- **Consumes**: `multipart/form-data`
- **Parts**:
    - `banner`: `CreateBannerRequest` (JSON)
    - `image`: File (Optional)

### Admin: Update Banner
- **URL**: `/api/admin/banners/{id}`
- **Method**: `PUT`
- **Consumes**: `multipart/form-data`
- **Parts**:
    - `banner`: `UpdateBannerRequest` (JSON)
    - `image`: File (Optional)

### Admin: Search Banners
- **URL**: `/api/admin/banners/search`
- **Method**: `POST`
- **Request Body**: `SearchRequest`

---

## 6. Public UI Configuration API

### Get All Public Configurations
- **URL**: `/api/public/ui/configs`
- **Method**: `GET`

### Get Configuration by Key
- **URL**: `/api/public/ui/configs/{key}`
- **Method**: `GET`
