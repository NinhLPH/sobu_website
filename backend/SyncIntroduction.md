# Nhanh.vn API v3 Synchronization Engine

This document provides a technical overview of the system designed to synchronize product data from Nhanh.vn into the local database.

---

## 1. Architecture Overview

The synchronization engine is built on a modular **Fetch → Map → Save** pipeline. This ensures that the logic for communicating with the external API is decoupled from the internal data representation and persistence logic.

### Core Components:
- **Service Layer (`ProductSyncService`)**: Orchestrates the sync process, handles pagination, and manages transactions.
- **Mapping Layer (`ProductMapper`)**: A pure component responsible for converting external DTOs into internal JPA entities.
- **DTO Layer (`com.vn.sodu.product.dto`)**: Strictly typed Java objects mirroring the Nhanh API v3 JSON structures.
- **Integration Management (`NhanhService`)**: Manages OAuth2 tokens and tracks the "Last Sync" state.

---

## 2. Data Models (DTOs vs. Entities)

### External Data (DTOs)
- **`NhanhProductDTO`**: The primary data container. It captures everything from basic info (name, code) to nested structures (prices, inventory, units, attributes, and images).
- **`NhanhProductListResponse`**: The root response object for Phase 1+ (POST) requests, providing structured access to the product list and pagination metadata (`totalPages`).

### Internal Entities
- **`Product`**: The main entity. Stores flattened data from Nhanh for performance (e.g., `brandName`, `categoryName` are cached directly on the product).
- **Child Entities**: `ProductUnit`, `ProductAttribute`, and `ProductImage` are stored in separate tables linked by `productId` to maintain relational integrity.

---

## 3. The Mapping Logic (`ProductMapper`)

The mapper performs several critical transformations:
- **External ID Mapping**: Maps Nhanh's `id` to `externalId` in our database. This is the **Unique Key** used for upsert logic.
- **Time Normalization**: Handles the conversion of Nhanh's UNIX timestamps into `LocalDateTime`. It is "smart" enough to detect if the source is in seconds or milliseconds.
- **Defensive Mapping**: Includes null-checks for every nested object (Category, Brand, Prices) to prevent `NullPointerException` during synchronization of incomplete product data.

---

## 4. Synchronization Logic (`ProductSyncService`)

### Incremental Synchronization
To optimize performance, the system uses an **Incremental Sync** strategy:
1. It retrieves `lastProductSyncTime` from the `NhanhIntegration` table.
2. It sends this timestamp to Nhanh using the `updatedAt` filter.
3. Only products modified after this timestamp are returned by the API.
4. After success, it updates the `lastProductSyncTime` to the current system time.

### Upsert & Child Management
For every product fetched:
1. **Find or Create**: It checks if a product with the same `externalId` exists.
2. **Transactional Sync**: The `syncOne` method is marked `@Transactional`. It saves the main `Product` and then:
    - Deletes all existing child records (units, attributes, images).
    - Re-inserts the fresh child records from the DTO.
3. This ensures that the local database is always a perfect mirror of the Nhanh source.

### Batch Processing & Resilience
- **Batch Saving**: Child entities are saved using a `saveInBatches` utility (chunk size: 100) to optimize database I/O.
- **Retry Mechanism**: API calls are wrapped in an exponential backoff retry helper to survive transient network failures.

---

## 5. Automation

The system is fully automated using Spring's task scheduling:
- **Cron Job**: Configurable via `nhanh.sync.cron` (default: hourly).
- **Manual Override**: The `POST api/admin/products/sync` endpoint allows administrators to trigger an immediate incremental sync.

---

## 6. Key Configuration Parameters

| Property | Description | Default |
| :--- | :--- | :--- |
| `nhanh.api.products-url` | The Nhanh V3 Product List endpoint | `.../v3.0/product/list` |
| `nhanh.sync.cron` | Schedule for automation | `0 0 * * * *` (Hourly) |
| `batchSize` | Chunk size for DB operations | `100` |
