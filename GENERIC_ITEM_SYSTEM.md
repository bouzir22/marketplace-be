# Generic Item System

## Overview

This marketplace backend implements a **generic item system** that can support all types of products through a flexible architecture:

- **Database (PostgreSQL)**: Stores only item ID and image URLs (minimal data)
- **Elasticsearch**: Stores all product attributes dynamically (name, price, category, etc.)
- **Remote Storage (AWS S3)**: Stores actual image files

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Item Controller                         │
│                     (REST API Layer)                         │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Item Service                           │
│                  (Business Logic Layer)                      │
└──────┬─────────────────┬──────────────────┬─────────────────┘
       │                 │                  │
       ▼                 ▼                  ▼
┌──────────────┐  ┌────────────────┐  ┌──────────────────┐
│  PostgreSQL  │  │ Elasticsearch  │  │  AWS S3 Storage  │
│              │  │                │  │                  │
│ • id         │  │ • id           │  │ • image files    │
│ • image      │  │ • attributes   │  │                  │
│ • images[]   │  │   (dynamic)    │  │                  │
│ • mapId      │  │                │  │                  │
└──────────────┘  └────────────────┘  └──────────────────┘
```

## Database Schema

### Item Entity (PostgreSQL)

```java
@Entity
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String attributesMapId;  // UUID for attribute mapping
    private String image;             // Main image URL

    @ElementCollection
    private List<String> images;      // Additional image URLs
}
```

## Elasticsearch Document

```java
@Document(indexName = "items")
public class ItemDocument {
    @Id
    private String id;  // Same as Item.id

    @Field(type = FieldType.Object)
    private Map<String, Object> attributes;  // Dynamic attributes
}
```

Example attributes:
```json
{
  "name": "iPhone 15 Pro",
  "brand": "Apple",
  "category": "Electronics",
  "price": 999.99,
  "condition": "New",
  "description": "Latest iPhone with titanium design",
  "size": "6.1 inch",
  "color": "Natural Titanium",
  "inStock": true
}
```

## API Endpoints

### 1. Create Item (with image upload)

**POST** `/items`
- Content-Type: `multipart/form-data`

**Form Data:**
- `mainImage` (file, optional): Main product image
- `additionalImages` (files, optional): Additional images
- `attributes` (JSON string, required): Product attributes

**Example using cURL:**
```bash
curl -X POST http://localhost:8080/items \
  -F "mainImage=@product-main.jpg" \
  -F "additionalImages=@image1.jpg" \
  -F "additionalImages=@image2.jpg" \
  -F 'attributes={"name":"iPhone 15 Pro","brand":"Apple","price":999.99,"category":"Electronics"}'
```

**Response:**
```json
{
  "id": 1,
  "image": "https://s3.amazonaws.com/bucket/uuid-main.jpg",
  "images": [
    "https://s3.amazonaws.com/bucket/uuid-1.jpg",
    "https://s3.amazonaws.com/bucket/uuid-2.jpg"
  ],
  "attributes": {
    "name": "iPhone 15 Pro",
    "brand": "Apple",
    "price": 999.99,
    "category": "Electronics"
  }
}
```

### 2. Create Item (JSON only)

**POST** `/items`
- Content-Type: `application/json`

**Request Body:**
```json
{
  "image": "https://example.com/image.jpg",
  "images": ["https://example.com/img1.jpg"],
  "attributes": {
    "name": "Product Name",
    "price": 99.99
  }
}
```

### 3. Get All Items

**GET** `/items/all`

**Response:**
```json
[
  {
    "id": 1,
    "image": "...",
    "images": ["..."],
    "attributes": {...}
  }
]
```

### 4. Get Item by ID

**GET** `/items/{id}`

**Response:**
```json
{
  "id": 1,
  "image": "...",
  "images": ["..."],
  "attributes": {...}
}
```

### 5. Search Items by Attribute

**GET** `/items/search?key=category&value=electronics`

Search items by any attribute stored in Elasticsearch.

**Examples:**
- `/items/search?key=brand&value=Apple`
- `/items/search?key=category&value=Electronics`
- `/items/search?key=condition&value=New`

### 6. Update Item

**PUT** `/items/{id}`
- Content-Type: `multipart/form-data`

**Form Data:**
- `mainImage` (file, optional): New main image
- `additionalImages` (files, optional): New additional images
- `attributes` (JSON string, optional): Updated attributes

### 7. Partial Update (Attributes Only)

**PATCH** `/items/{id}`
- Content-Type: `application/json`

**Request Body:**
```json
{
  "price": 899.99,
  "inStock": false
}
```

Only updates the specified attributes without touching images.

### 8. Delete Item

**DELETE** `/items/{id}`

Deletes:
- Database record
- Elasticsearch document
- All images from S3

### 9. Delete Multiple Items

**DELETE** `/items`

**Request Body:**
```json
[1, 2, 3, 4, 5]
```

## Configuration

### Environment Variables

#### Database (PostgreSQL)
```properties
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
```

#### Elasticsearch
```properties
ELASTICSEARCH_URIS=http://elasticsearch:9200
ELASTICSEARCH_USERNAME=elastic_user  # optional
ELASTICSEARCH_PASSWORD=elastic_pass  # optional
```

#### AWS S3
```properties
S3_ENABLED=true
S3_ACCESS_KEY=your_access_key
S3_SECRET_KEY=your_secret_key
S3_BUCKET=marketplace-images
S3_REGION=us-east-1
S3_ENDPOINT=  # For S3-compatible services like MinIO
```

### Docker Compose Example

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: _ach_db
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}

  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"

  backend:
    build: .
    environment:
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      ELASTICSEARCH_URIS: http://elasticsearch:9200
      S3_ENABLED: true
      S3_ACCESS_KEY: ${S3_ACCESS_KEY}
      S3_SECRET_KEY: ${S3_SECRET_KEY}
    depends_on:
      - postgres
      - elasticsearch
```

## Benefits

1. **Flexibility**: Support any product type without schema changes
2. **Scalability**: Elasticsearch provides fast search across all attributes
3. **Performance**: Minimal database storage, efficient queries
4. **Storage Optimization**: Images stored separately in S3
5. **Search Capabilities**: Full-text search and filtering on any attribute

## Development Notes

### S3 Mock Mode

When `S3_ENABLED=false`, the system generates mock URLs for testing:
```
https://mock-storage.example.com/images/{uuid}.jpg
```

### Adding New Product Types

No code changes needed! Just pass different attributes:

**Electronics:**
```json
{
  "name": "...",
  "brand": "...",
  "price": 999,
  "warranty": "2 years"
}
```

**Clothing:**
```json
{
  "name": "...",
  "brand": "...",
  "size": "M",
  "color": "Blue",
  "material": "Cotton"
}
```

**Books:**
```json
{
  "title": "...",
  "author": "...",
  "isbn": "...",
  "publisher": "..."
}
```

## Testing

### Test Item Creation with cURL

```bash
# Create an item
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -d '{
    "image": "https://example.com/image.jpg",
    "attributes": {
      "name": "Test Product",
      "price": 49.99,
      "category": "Test"
    }
  }'

# Search by attribute
curl "http://localhost:8080/items/search?key=category&value=Test"

# Get item by ID
curl http://localhost:8080/items/1

# Update attributes
curl -X PATCH http://localhost:8080/items/1 \
  -H "Content-Type: application/json" \
  -d '{"price": 39.99}'

# Delete item
curl -X DELETE http://localhost:8080/items/1
```

## Migration Guide

If you have existing product-specific tables, migrate them to the generic system:

1. Export existing product data
2. For each product:
   - Keep only `id`, `image`, `images` in database
   - Move all other fields to `attributes` map in Elasticsearch
3. Upload images to S3 and update URLs

## Future Enhancements

- [ ] Advanced Elasticsearch queries (full-text search, aggregations)
- [ ] Image optimization and resizing
- [ ] Attribute validation schemas per product type
- [ ] Bulk import/export functionality
- [ ] Search suggestions and autocomplete
- [ ] Multi-language attribute support
