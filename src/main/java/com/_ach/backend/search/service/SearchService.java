package com._ach.backend.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com._ach.backend.Model.ItemImageDTO;
import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.entity.Item;
import com._ach.backend.entity.ItemImage;
import com._ach.backend.repository.ItemRepository;
import com._ach.backend.search.document.ItemDocument;
import com._ach.backend.search.dto.ItemSearchResponse;
import com._ach.backend.search.dto.SearchResponse.SearchHit;
import com._ach.backend.service.ItemDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingService embeddingService;
    private final ItemRepository itemRepository;
    private final ItemDetailsService itemDetailsService;

    @Value("${elasticsearch.index.name}")
    private String indexName;

    /**
     * Executes a search based on the search type specified in the request
     */
    public com._ach.backend.search.dto.SearchResponse search(com._ach.backend.search.dto.SearchRequest searchRequest) {
        try {
            long startTime = System.currentTimeMillis();

            com._ach.backend.search.dto.SearchResponse response = switch (searchRequest.getSearchType().toLowerCase()) {
                case "fuzzy" -> fuzzySearch(searchRequest);
                case "semantic" -> semanticSearch(searchRequest);
                case "hybrid" -> hybridSearch(searchRequest);
                default -> throw new IllegalArgumentException("Invalid search type: " + searchRequest.getSearchType());
            };

            long took = System.currentTimeMillis() - startTime;
            response.setTook(took);

            return response;
        } catch (IOException e) {
            log.error("Error executing search", e);
            throw new RuntimeException("Search execution failed", e);
        }
    }

    /**
     * Search by attribute key-value pairs - returns ItemRepresentation
     */
    public ItemSearchResponse searchByAttributes(Map<String, Object> filters, int page, int size) {
        try {
            long startTime = System.currentTimeMillis();

            Query query;
            if (filters == null || filters.isEmpty()) {
                query = Query.of(q -> q.matchAll(m -> m));
            } else {
                List<Query> filterQueries = new ArrayList<>();
                for (Map.Entry<String, Object> entry : filters.entrySet()) {
                    String field = "attributes." + entry.getKey();
                    String value = entry.getValue().toString();

                    Query filterQuery = Query.of(q -> q
                            .wildcard(w -> w
                                    .field(field)
                                    .value("*" + value + "*")
                                    .caseInsensitive(true)
                            )
                    );
                    filterQueries.add(filterQuery);
                }
                query = Query.of(q -> q.bool(b -> b.must(filterQueries)));
            }

            SearchRequest esRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(query)
                    .from(page * size)
                    .size(size)
            );

            long esStartTime = System.currentTimeMillis();
            SearchResponse<ItemDocument> esResponse = elasticsearchClient.search(esRequest, ItemDocument.class);
            long esQueryTime = System.currentTimeMillis() - esStartTime;
            long esInternalTime = esResponse.took();

            log.debug("Elasticsearch query time: {}ms (internal: {}ms)", esQueryTime, esInternalTime);

            // Extract ES doc IDs and attributes from search response (no second ES call needed)
            Map<String, Map<String, Object>> attributesMap = new HashMap<>();
            List<String> esDocIds = new ArrayList<>();

            for (Hit<ItemDocument> hit : esResponse.hits().hits()) {
                esDocIds.add(hit.id());
                if (hit.source() != null && hit.source().getAttributes() != null) {
                    attributesMap.put(hit.id(), hit.source().getAttributes());
                }
            }

            // Fetch items from database
            long dbStartTime = System.currentTimeMillis();
            List<Item> items = itemRepository.findByAttributesMapIdIn(esDocIds);
            long dbQueryTime = System.currentTimeMillis() - dbStartTime;

            log.debug("Database query time: {}ms", dbQueryTime);

            // Convert to ItemRepresentation using attributes from search response
            long conversionStartTime = System.currentTimeMillis();
            List<ItemRepresentation> itemRepresentations = items.stream()
                    .map(item -> toRepresentation(item, attributesMap))
                    .toList();
            long conversionTime = System.currentTimeMillis() - conversionStartTime;

            log.debug("Conversion time: {}ms", conversionTime);

            long totalTime = System.currentTimeMillis() - startTime;
            log.debug("Total response time: {}ms (ES: {}ms, DB: {}ms, Conversion: {}ms)", totalTime, esQueryTime, dbQueryTime, conversionTime);

            return new ItemSearchResponse(
                    itemRepresentations,
                    esResponse.hits().total() != null ? esResponse.hits().total().value() : 0L,
                    page,
                    size,
                    totalTime
            );
        } catch (IOException e) {
            log.error("Error executing attribute search", e);
            throw new RuntimeException("Attribute search failed", e);
        }
    }

    private ItemRepresentation toRepresentation(Item item, Map<String, Map<String, Object>> attributesMap) {
        ItemRepresentation representation = new ItemRepresentation();
        representation.setId(item.getId());

        List<ItemImageDTO> imageDTOs = item.getImages().stream()
                .map(this::toImageDTO)
                .toList();
        representation.setImages(imageDTOs);

        if (item.getAttributesMapId() != null) {
            Map<String, Object> attributes = attributesMap.get(item.getAttributesMapId());
            if (attributes != null) {
                representation.setAttributes(attributes);
            }
        }

        return representation;
    }

    private ItemImageDTO toImageDTO(ItemImage image) {
        ItemImageDTO dto = new ItemImageDTO();
        dto.setId(image.getId());
        dto.setUrl(image.getUrl());
        dto.setMain(image.isMain());
        dto.setDisplayOrder(image.getDisplayOrder());
        dto.setAltText(image.getAltText());
        return dto;
    }

    /**
     * Performs fuzzy search using Elasticsearch fuzzy query
     */
    private com._ach.backend.search.dto.SearchResponse fuzzySearch(
            com._ach.backend.search.dto.SearchRequest searchRequest) throws IOException {

        // Build the fuzzy query
        Query fuzzyQuery = buildFuzzyQuery(searchRequest);

        // Apply filters if provided
        Query finalQuery = applyFilters(fuzzyQuery, searchRequest.getFilters());

        // Execute search
        SearchRequest esRequest = SearchRequest.of(s -> s
                .index(indexName)
                .query(finalQuery)
                .from(searchRequest.getPage() * searchRequest.getSize())
                .size(searchRequest.getSize())
                .minScore(searchRequest.getMinScore() != null ? searchRequest.getMinScore().doubleValue() : null)
        );

        SearchResponse<ItemDocument> esResponse = elasticsearchClient.search(esRequest, ItemDocument.class);

        return buildSearchResponse(esResponse, searchRequest, "fuzzy");
    }

    /**
     * Performs semantic search using vector similarity
     */
    private com._ach.backend.search.dto.SearchResponse semanticSearch(
            com._ach.backend.search.dto.SearchRequest searchRequest) throws IOException {

        // Generate embedding for the query
        float[] queryEmbedding = embeddingService.generateEmbedding(searchRequest.getQuery());

        // Build KNN query for semantic search
        Query knnQuery = Query.of(q -> q
                .scriptScore(ss -> ss
                        .query(Query.of(mq -> mq.matchAll(m -> m)))
                        .script(s -> s.source("cosineSimilarity(params.query_vector, 'embedding') + 1.0").params("query_vector", JsonData.of(queryEmbedding)))
                )
        );

        // Apply filters if provided
        Query finalQuery = applyFilters(knnQuery, searchRequest.getFilters());

        SearchRequest esRequest = SearchRequest.of(s -> s
                .index(indexName)
                .query(finalQuery)
                .from(searchRequest.getPage() * searchRequest.getSize())
                .size(searchRequest.getSize())
                .minScore(searchRequest.getMinScore() != null ? searchRequest.getMinScore().doubleValue() : null)
        );

        SearchResponse<ItemDocument> esResponse = elasticsearchClient.search(esRequest, ItemDocument.class);

        return buildSearchResponse(esResponse, searchRequest, "semantic");
    }

    /**
     * Performs hybrid search combining fuzzy and semantic search
     */
    private com._ach.backend.search.dto.SearchResponse hybridSearch(
            com._ach.backend.search.dto.SearchRequest searchRequest) throws IOException {

        // Generate embedding for semantic component
        float[] queryEmbedding = embeddingService.generateEmbedding(searchRequest.getQuery());

        // Build fuzzy query
        Query fuzzyQuery = buildFuzzyQuery(searchRequest);

        // Build semantic query
        Query semanticQuery = Query.of(q -> q
                .scriptScore(ss -> ss
                        .query(Query.of(mq -> mq.matchAll(m -> m)))
                        .script(s -> s.source("cosineSimilarity(params.query_vector, 'embedding') + 1.0").params("query_vector", JsonData.of(queryEmbedding)))
                )
        );

        // Combine using bool should query
        Query combinedQuery = Query.of(q -> q
                .bool(b -> b
                        .should(fuzzyQuery)
                        .should(semanticQuery)
                )
        );

        // Apply filters
        Query finalQuery = applyFilters(combinedQuery, searchRequest.getFilters());

        SearchRequest esRequest = SearchRequest.of(s -> s
                .index(indexName)
                .query(finalQuery)
                .from(searchRequest.getPage() * searchRequest.getSize())
                .size(searchRequest.getSize())
                .minScore(searchRequest.getMinScore() != null ? searchRequest.getMinScore().doubleValue() : null)
        );

        SearchResponse<ItemDocument> esResponse = elasticsearchClient.search(esRequest, ItemDocument.class);

        return buildSearchResponse(esResponse, searchRequest, "hybrid");
    }

    /**
     * Builds a fuzzy query with multi-match across multiple fields
     */
    private Query buildFuzzyQuery(com._ach.backend.search.dto.SearchRequest searchRequest) {
        return Query.of(q -> q
                .queryString(qs -> qs
                        .query("*" + searchRequest.getQuery() + "*")
                        .fields("attributes.*")
                        .defaultOperator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
                        .lenient(true)
                )
        );
    }

    /**
     * Applies filters to the query using bool must clauses
     */
    private Query applyFilters(Query baseQuery, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return baseQuery;
        }

        List<Query> filterQueries = new ArrayList<>();
        filterQueries.add(baseQuery);

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String field = "attributes." + entry.getKey();
            Object value = entry.getValue();

            Query filterQuery = Query.of(q -> q
                    .term(t -> t
                            .field(field)
                            .value(v -> v.stringValue(value.toString()))
                    )
            );
            filterQueries.add(filterQuery);
        }

        return Query.of(q -> q
                .bool(b -> b.must(filterQueries))
        );
    }

    /**
     * Builds the search response from Elasticsearch response
     */
    private com._ach.backend.search.dto.SearchResponse buildSearchResponse(
            SearchResponse<ItemDocument> esResponse,
            com._ach.backend.search.dto.SearchRequest searchRequest,
            String searchType) {

        List<SearchHit> hits = esResponse.hits().hits().stream()
                .map(hit -> {
                    ItemDocument doc = hit.source();
                    if (doc != null) {
                        doc.setId(hit.id());
                    }
                    return new SearchHit(
                            doc,
                            hit.score() != null ? hit.score().floatValue() : 0.0f,
                            searchType
                    );
                })
                .collect(Collectors.toList());

        return new com._ach.backend.search.dto.SearchResponse(
                hits,
                esResponse.hits().total() != null ? esResponse.hits().total().value() : 0L,
                searchRequest.getPage(),
                searchRequest.getSize(),
                0L // Will be set by the caller
        );
    }

    /**
     * Indexes a document in Elasticsearch
     */
    public void indexDocument(ItemDocument document) {
        try {
            // Generate embedding if searchable content is available
            if (document.getSearchableContent() != null && !document.getSearchableContent().isEmpty()) {
                float[] embedding = embeddingService.generateEmbedding(document.getSearchableContent());
                document.setEmbedding(embedding);
            }

            document.setUpdatedAt(System.currentTimeMillis());
            if (document.getCreatedAt() == null) {
                document.setCreatedAt(System.currentTimeMillis());
            }

            elasticsearchClient.index(i -> i
                    .index(indexName)
                    .id(document.getId())
                    .document(document)
            );

            log.info("Document indexed successfully: {}", document.getId());
        } catch (IOException e) {
            log.error("Error indexing document", e);
            throw new RuntimeException("Failed to index document", e);
        }
    }

    /**
     * Deletes a document from Elasticsearch
     */
    public void deleteDocument(String id) {
        try {
            elasticsearchClient.delete(d -> d
                    .index(indexName)
                    .id(id)
            );
            log.info("Document deleted successfully: {}", id);
        } catch (IOException e) {
            log.error("Error deleting document", e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
}
