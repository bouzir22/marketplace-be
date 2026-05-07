# Elasticsearch Search Service

This document describes the Elasticsearch-based search service that supports both fuzzy search and semantic search capabilities.

## Features

- **Fuzzy Search**: Traditional text search with fuzzy matching for typo tolerance
- **Semantic Search**: Vector-based similarity search for meaning-based queries
- **Hybrid Search**: Combines both fuzzy and semantic search for best results
- **Advanced Filtering**: Filter results by product_details
- **Pagination**: Built-in pagination support
- **Score Thresholding**: Filter results by minimum relevance score

## Architecture

### Components

1. **ItemDocument**: Elasticsearch document entity with vector embedding support
2. **SearchService**: Core search logic implementing fuzzy, semantic, and hybrid search
3. **EmbeddingService**: Generates vector embeddings for semantic search
4. **SearchController**: REST API endpoints
5. **ElasticsearchConfig**: Elasticsearch client configuration

### Index Structure

The `items` index contains the following fields:

- `id`: Unique identifier
- `productDetailsId`: Text field with standard analyzer
- `image`: Main image URL (keyword)
- `images`: List of additional image URLs
- `product_details`: Dynamic object for item product_details
- `searchableContent`: Searchable text content
- `embedding`: 768-dimensional dense vector for semantic search
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp

## Setup

### Prerequisites

- Elasticsearch 8.x or higher
- Java 17+
- Maven

### Elasticsearch Setup

1. **Start Elasticsearch**:
```bash
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  elasticsearch:8.11.0
```

2. **Create the index with proper mappings**:
```bash
curl -X PUT "localhost:9200/items" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "attributesMapId": {
        "type": "text",
        "analyzer": "standard"
      },
      "image": { "type": "keyword" },
      "images": { "type": "keyword" },
      "product_details": {
        "type": "object",
        "dynamic": true
      },
      "searchableContent": {
        "type": "text",
        "analyzer": "standard"
      },
      "embedding": {
        "type": "dense_vector",
        "dims": 768,
        "index": true,
        "similarity": "cosine"
      },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  },
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 1,
    "index": {
      "similarity": {
        "default": {
          "type": "BM25"
        }
      }
    }
  }
}
'
```

3. **Configure environment variables**:
```bash
export ELASTICSEARCH_HOST=localhost
export ELASTICSEARCH_PORT=9200
export ELASTICSEARCH_USERNAME=  # Optional
export ELASTICSEARCH_PASSWORD=  # Optional
```

## API Usage

### 1. Simple Search (GET)

Basic search with query parameters:

```bash
# Fuzzy search
curl "http://localhost:8080/api/search?query=laptop&searchType=fuzzy&page=0&size=10"

# Semantic search
curl "http://localhost:8080/api/search?query=portable%20computer&searchType=semantic"

# Hybrid search
curl "http://localhost:8080/api/search?query=gaming%20laptop&searchType=hybrid"
```

Parameters:
- `query` (required): Search query text
- `searchType` (optional): "fuzzy", "semantic", or "hybrid" (default: "fuzzy")
- `fuzziness` (optional): 0-2 (default: 2)
- `page` (optional): Page number (default: 0)
- `size` (optional): Results per page (default: 10)
- `minScore` (optional): Minimum relevance score threshold

### 2. Advanced Search (POST)

Complex search with filters and custom parameters:

```bash
curl -X POST "http://localhost:8080/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "laptop",
    "searchType": "hybrid",
    "fuzziness": 2,
    "page": 0,
    "size": 20,
    "minScore": 0.5,
    "filters": {
      "brand": "Dell",
      "category": "Electronics"
    },
    product_detailsBoost: 2.0,
    "contentBoost": 1.5
  }'
```

Request body:
```json
{
  "query": "search text",
  "searchType": "fuzzy|semantic|hybrid",
  "fuzziness": 0-2,
  "page": 0,
  "size": 10,
  "minScore": 0.0,
  "filters": {
    "key": "value"
  },
  product_detailsBoost: 1.0,
  "contentBoost": 1.0
}
```

Response:
```json
{
  "hits": [
    {
      "document": {
        "id": "1",
        "productDetailsId": "map123",
        "image": "image.jpg",
        "images": ["img1.jpg", "img2.jpg"],
        "product_details": {
          "brand": "Dell",
          "category": "Electronics"
        },
        "searchableContent": "Dell laptop with 16GB RAM",
        "createdAt": 1234567890,
        "updatedAt": 1234567890
      },
      "score": 0.95,
      "searchType": "hybrid"
    }
  ],
  "totalHits": 100,
  "page": 0,
  "size": 10,
  "took": 45
}
```

### 3. Index Document

Add or update a document in the search index:

```bash
curl -X POST "http://localhost:8080/api/search/index" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "1",
    "productDetailsId": "map123",
    "image": "laptop.jpg",
    "images": ["laptop1.jpg", "laptop2.jpg"],
    "product_details": {
      "brand": "Dell",
      "model": "XPS 15",
      "category": "Electronics",
      "price": 1299.99
    },
    "searchableContent": "Dell XPS 15 laptop with Intel Core i7, 16GB RAM, 512GB SSD"
  }'
```

Note: The embedding vector is automatically generated from `searchableContent`.

### 4. Delete Document

Remove a document from the search index:

```bash
curl -X DELETE "http://localhost:8080/api/search/1"
```

## Search Types Explained

### Fuzzy Search

Uses Elasticsearch's multi-match query with fuzziness to handle typos and variations:

- Best for: Exact product names, SKUs, or known terms
- Handles: Typos, misspellings (controlled by fuzziness parameter)
- Example: "lapto" will match "laptop"

### Semantic Search

Uses vector embeddings and cosine similarity to find semantically similar items:

- Best for: Conceptual queries, descriptions, natural language
- Handles: Synonyms, related concepts, meaning-based matching
- Example: "portable computer" will match "laptop"

**Note**: The current implementation uses a placeholder embedding service. For production:
- Integrate with OpenAI Embeddings API
- Use Sentence Transformers
- Deploy a custom embedding model

### Hybrid Search

Combines both fuzzy and semantic search for comprehensive results:

- Best for: General search where you want both exact and semantic matches
- Handles: Both typos and conceptual similarity
- Example: "gaming lapto" will match both "gaming laptop" (fuzzy) and "high-performance computer for games" (semantic)

## Embedding Service Integration

The `EmbeddingService` currently uses a placeholder implementation. To integrate with a real embedding service:

### Option 1: OpenAI Embeddings

1. Add OpenAI client dependency to `pom.xml`:
```xml
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
    <version>0.18.2</version>
</dependency>
```

2. Update `EmbeddingService.java`:
```java
@Value("${openai.api.key}")
private String openaiApiKey;

public float[] generateEmbedding(String text) {
    OpenAiService service = new OpenAiService(openaiApiKey);
    EmbeddingRequest request = EmbeddingRequest.builder()
        .model("text-embedding-ada-002")
        .input(Collections.singletonList(text))
        .build();
    List<Embedding> embeddings = service.createEmbeddings(request).getData();

    List<Double> embedding = embeddings.get(0).getEmbedding();
    float[] result = new float[embedding.size()];
    for (int i = 0; i < embedding.size(); i++) {
        result[i] = embedding.get(i).floatValue();
    }
    return result;
}
```

### Option 2: Sentence Transformers (via REST API)

Deploy a Sentence Transformers model and call it via REST:

```java
public float[] generateEmbedding(String text) {
    RestTemplate restTemplate = new RestTemplate();
    Map<String, String> request = Map.of("text", text);

    ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
        embeddingServiceUrl + "/embed",
        request,
        EmbeddingResponse.class
    );

    return response.getBody().getEmbedding();
}
```

## Performance Considerations

1. **Indexing**: Use bulk indexing for large datasets
2. **Caching**: Consider caching frequently searched queries
3. **Sharding**: Adjust number of shards based on data volume
4. **Replicas**: Configure replicas for high availability
5. **Refresh Interval**: Tune refresh interval for write-heavy workloads

## Monitoring

Monitor these metrics:

- Search latency (check `took` field in responses)
- Index size and document count
- Query cache hit rate
- Elasticsearch cluster health

## Troubleshooting

### Connection Issues

```bash
# Check Elasticsearch is running
curl http://localhost:9200/_cluster/health

# Check index exists
curl http://localhost:9200/items
```

### No Results

- Check if documents are indexed: `curl http://localhost:9200/items/_count`
- Verify search query syntax
- Try lowering `minScore` threshold
- Check if filters are too restrictive

### Low Relevance Scores

- Adjust boost parameters (`product_detailsBoost`, `contentBoost`)
- Fine-tune fuzziness level
- For semantic search, ensure embeddings are properly generated

## Future Enhancements

- [ ] Integrate with real embedding service (OpenAI, HuggingFace)
- [ ] Add query suggestions and autocomplete
- [ ] Implement result highlighting
- [ ] Add aggregations for faceted search
- [ ] Support for multiple languages
- [ ] Query analytics and optimization
- [ ] A/B testing for search relevance
