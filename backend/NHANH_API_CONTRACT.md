# Nhanh.vn API Contract - Product Synchronization

## 1. Product Search API
**Endpoint:** `GET /api/product/search`

### Request Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `appId` | Integer | Yes | Your App ID |
| `businessId` | Integer | Yes | Business ID from token |
| `accessToken` | String | Yes | Access token |
| `page` | Integer | No | Page number (default: 1) |
| `pageSize` | Integer | No | Items per page (max: 50) |
| `updatedAt` | String | No | Filter by update time (YYYY-MM-DD HH:mm:ss) |

### Response Structure
```json
{
  "code": 1,
  "data": {
    "products": [
      {
        "id": "Long",
        "parentId": "Long",
        "code": "String",
        "barcode": "String",
        "name": "String",
        "otherName": "String",
        "status": "String",
        "vat": "Integer",
        "category": { "id": "Long", "code": "String", "name": "String" },
        "brand": { "id": "Long", "name": "String" },
        "type": { "id": "Long", "name": "String" },
        "prices": {
          "retail": "Double",
          "wholesale": "Double",
          "importPrice": "Double",
          "old": "Double",
          "avgCost": "Double"
        },
        "images": { "avatar": "String", "others": ["String"] },
        "inventory": { "remain": "Double", "available": "Double" },
        "updatedAt": "Long",
        "createdAt": "Long",
        "description": "String",
        "content": "String",
        "childs": "List<NhanhProductDTO>"
      }
    ],
    "page": 1,
    "totalPages": 10
  }
}
```

## 2. Implementation Notes
- Do NOT use `List<NhanhProductDTO>` directly in `NhanhResponse<T>`.
- Use `NhanhProductListData` as the generic type `T`.
- Ensure `page` and `totalPages` are correctly mapped from the `data` object.
