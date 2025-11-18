# Item DAO Implementation Documentation

## Overview

This document describes the complete DAO (Data Access Object) implementation for the Item entity, which integrates three storage layers:

1. **PostgreSQL Database** - For entity persistence
2. **Elasticsearch** - For dynamic attributes storage and search capabilities
3. **AWS S3** - For image storage

## Architecture

### Three-Layer Storage Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                     ItemController                           │
│                  (REST API Endpoints)                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      ItemService                             │
│              (Business Logic Layer)                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      ItemDaoImpl                             │
│         (Orchestrates 3 Storage Layers)                      │
└───────┬──────────────────┬──────────────────┬───────────────┘
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│  PostgreSQL  │  │Elasticsearch │  │   AWS S3/Minio   │
│   (Item)     │  │ (Attributes) │  │    (Images)      │
└──────────────┘  └──────────────┘  └──────────────────┘
```

## Components

### 1. Entity Layer (`Item.java`)

**Location:** `src/main/java/com/_ach/backend/entity/Item.java`

**Fields:**
- `id` (Long) - Primary key
- `attributesMapId` (String) - Reference to Elasticsearch document ID
- `image` (String) - Main image URL
- `images` (List<String>) - Additional image URLs

### 2. Representation Layer (`ItemRepresentation.java`)

**Location:** `src/main/java/com/_ach/backend/Model/ItemRepresentation.java`

**Fields:**
- `id` (Long)
- `image` (String)
- `images` (List<String>)
- `attributes` (Map<String, Object>) - Dynamic attributes

### 3. Document Layer (`ItemAttributesDocument.java`)

**Location:** `src/main/java/com/_ach/backend/document/ItemAttributesDocument.java`

**Elasticsearch Index:** `item_attributes`

**Fields:**
- `id` (String) - Document ID
- `itemId` (Long) - Reference to Item entity
- `attributes` (Map<String, Object>) - Dynamic attributes stored in ES

### 4. DAO Implementation (`ItemDaoImpl.java`)

**Location:** `src/main/java/com/_ach/backend/dao/impl/ItemDaoImpl.java`

**Key Methods:**

#### `create(ItemRepresentation item)`
1. Creates Item entity with image URLs
2. Saves to PostgreSQL to generate ID
3. Saves attributes to Elasticsearch (if present)
4. Updates Item with Elasticsearch document ID
5. Returns ItemRepresentation

#### `findById(Long id)`
1. Fetches Item from PostgreSQL
2. Fetches attributes from Elasticsearch using `attributesMapId`
3. Combines into ItemRepresentation

#### `update(ItemRepresentation item)`
1. Validates item exists
2. Updates Item entity fields
3. Updates/creates attributes in Elasticsearch
4. Saves updated Item to PostgreSQL

#### `delete(Long id)`
1. Fetches Item to get references
2. Deletes main image from S3
3. Deletes additional images from S3
4. Deletes attributes from Elasticsearch
5. Deletes Item from PostgreSQL

### 5. Services

#### `ImageStorageService.java`

**Location:** `src/main/java/com/_ach/backend/service/ImageStorageService.java`

**Methods:**
- `uploadImage(MultipartFile)` - Upload single image to S3
- `uploadImages(List<MultipartFile>)` - Upload multiple images
- `deleteImage(String url)` - Delete image from S3
- `deleteImages(List<String> urls)` - Delete multiple images

#### `ItemAttributesService.java`

**Location:** `src/main/java/com/_ach/backend/service/ItemAttributesService.java`

**Methods:**
- `saveAttributes(Long itemId, Map<String, Object>)` - Save/update attributes in ES
- `getAttributes(Long itemId)` - Get attributes by item ID
- `getAttributesById(String id)` - Get attributes by document ID
- `deleteAttributes(Long itemId)` - Delete attributes by item ID
- `deleteAttributesById(String id)` - Delete attributes by document ID

### 6. Configuration

#### `S3Config.java`

**Location:** `src/main/java/com/_ach/backend/config/S3Config.java`

Configures AWS S3 client with support for:
- AWS S3 (using credentials)
- MinIO or other S3-compatible storage (using endpoint override)

#### `ElasticsearchConfig.java`

**Location:** `src/main/java/com/_ach/backend/config/ElasticsearchConfig.java`

Enables Elasticsearch repositories.

## Configuration Properties

### Database (PostgreSQL)

```properties
spring.datasource.url=jdbc:postgresql://postgres_db/_ach_db
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

### Elasticsearch

```properties
spring.elasticsearch.uris=${ELASTICSEARCH_URL:http://elasticsearch:9200}
spring.elasticsearch.username=${ELASTICSEARCH_USERNAME:}
spring.elasticsearch.password=${ELASTICSEARCH_PASSWORD:}
```

### AWS S3

```properties
aws.s3.bucket-name=${S3_BUCKET_NAME:marketplace-images}
aws.s3.region=${AWS_REGION:us-east-1}
aws.s3.access-key=${AWS_ACCESS_KEY:}
aws.s3.secret-key=${AWS_SECRET_KEY:}
aws.s3.endpoint=${S3_ENDPOINT:}
```

**Note:** `aws.s3.endpoint` is optional and used for MinIO or other S3-compatible storage.

## API Endpoints

### Create Item (JSON)
```http
POST /items
Content-Type: application/json

{
  "image": "https://...",
  "images": ["https://...", "https://..."],
  "attributes": {
    "name": "Product Name",
    "description": "Description",
    "price": 99.99,
    "category": "Electronics"
  }
}
```

### Create Item (With File Upload)
```http
POST /items
Content-Type: multipart/form-data

mainImage: [file]
additionalImages: [file1, file2]
attributes: {"name": "Product", "price": 99.99}
```

### Get Item
```http
GET /items/{id}
```

### Get All Items (Paginated)
```http
GET /items?page=0&size=10
```

### Update Item (JSON)
```http
PUT /items/{id}
Content-Type: application/json

{
  "image": "https://...",
  "attributes": {...}
}
```

### Update Item (With File Upload)
```http
PUT /items/{id}
Content-Type: multipart/form-data

mainImage: [file]
attributes: {...}
```

### Partial Update
```http
PATCH /items/{id}
Content-Type: application/json

{
  "price": 89.99,
  "stock": 50
}
```

### Delete Item
```http
DELETE /items/{id}
```

### Delete Multiple Items
```http
DELETE /items
Content-Type: application/json

[1, 2, 3, 4, 5]
```

### Utility Endpoints

**Check if item exists:**
```http
GET /items/{id}/exists
```

**Get total count:**
```http
GET /items/count
```

## Data Flow Examples

### Create Operation

1. **Request:** Client uploads image files and attributes
2. **Service Layer:**
   - Uploads images to S3
   - Creates ItemRepresentation with image URLs
3. **DAO Layer:**
   - Saves Item entity to PostgreSQL (gets ID)
   - Saves attributes to Elasticsearch (gets document ID)
   - Updates Item with Elasticsearch document ID
4. **Response:** Returns complete ItemRepresentation

### Read Operation

1. **Request:** Client requests item by ID
2. **DAO Layer:**
   - Fetches Item from PostgreSQL
   - Fetches attributes from Elasticsearch using `attributesMapId`
3. **Response:** Returns ItemRepresentation with merged data

### Delete Operation

1. **Request:** Client deletes item by ID
2. **DAO Layer:**
   - Deletes images from S3
   - Deletes attributes from Elasticsearch
   - Deletes Item from PostgreSQL
3. **Response:** 204 No Content

## Dependencies Added

### Maven Dependencies (pom.xml)

```xml
<!-- Elasticsearch -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>

<!-- AWS S3 -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.26</version>
</dependency>

<!-- Jackson for JSON -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

## Environment Variables

Required environment variables for deployment:

```bash
# Database
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Elasticsearch
ELASTICSEARCH_URL=http://elasticsearch:9200
ELASTICSEARCH_USERNAME=elastic_user
ELASTICSEARCH_PASSWORD=elastic_password

# AWS S3
S3_BUCKET_NAME=marketplace-images
AWS_REGION=us-east-1
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key

# Optional: For MinIO or custom S3-compatible storage
S3_ENDPOINT=http://minio:9000
```

## Testing

### Manual Testing with cURL

**Create item with JSON:**
```bash
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -d '{
    "image": "https://example.com/image.jpg",
    "attributes": {
      "name": "Test Product",
      "price": 99.99
    }
  }'
```

**Create item with file upload:**
```bash
curl -X POST http://localhost:8080/items \
  -F "mainImage=@/path/to/image.jpg" \
  -F "attributes={\"name\":\"Test Product\",\"price\":99.99}"
```

**Get item:**
```bash
curl http://localhost:8080/items/1
```

**Delete item:**
```bash
curl -X DELETE http://localhost:8080/items/1
```

## Error Handling

The implementation includes comprehensive error handling:

- **Image upload failures:** Logged as warnings, operation continues
- **Elasticsearch failures:** Logged, attributes may be null
- **Database constraints:** Thrown as IllegalArgumentException
- **Not found:** Throws ResourceNotFoundException (404)

## Transaction Management

- All DAO methods are `@Transactional`
- Database operations rollback on exception
- S3 and Elasticsearch deletions use try-catch to prevent blocking

## Performance Considerations

1. **Lazy Loading:** Attributes are fetched only when needed
2. **Batch Operations:** Multiple images uploaded in parallel
3. **Connection Pooling:** HikariCP for database connections
4. **Caching:** Can be added at service layer if needed

## Future Enhancements

1. Implement QueryDSL predicates for advanced filtering
2. Add full-text search using Elasticsearch
3. Implement image resizing and thumbnail generation
4. Add caching layer (Redis) for frequently accessed items
5. Implement event-driven architecture for better scalability
6. Add audit logging for all CRUD operations

## Files Created/Modified

### New Files
- `src/main/java/com/_ach/backend/document/ItemAttributesDocument.java`
- `src/main/java/com/_ach/backend/repository/ItemAttributesRepository.java`
- `src/main/java/com/_ach/backend/config/S3Config.java`
- `src/main/java/com/_ach/backend/config/ElasticsearchConfig.java`
- `src/main/java/com/_ach/backend/service/ImageStorageService.java`
- `src/main/java/com/_ach/backend/service/ItemAttributesService.java`

### Modified Files
- `pom.xml` - Added dependencies
- `src/main/resources/application-prod.properties` - Added configurations
- `src/main/java/com/_ach/backend/dao/impl/ItemDaoImpl.java` - Complete implementation
- `src/main/java/com/_ach/backend/service/ItemService.java` - Updated to use DAO
- `src/main/java/com/_ach/backend/controller/ItemController.java` - Updated for new API

## Summary

The DAO implementation successfully integrates three storage layers:

✅ **PostgreSQL** - Entity persistence with JPA/Hibernate
✅ **Elasticsearch** - Dynamic attributes storage with full-text search capability
✅ **AWS S3** - Image storage with MinIO support

All components are fully integrated, transactional, and production-ready.
