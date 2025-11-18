package com._ach.backend.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com._ach.backend.search.document.ItemDocument;
import com._ach.backend.search.dto.SearchResponse.SearchHit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingService embeddingService;

    /**
     * Executes a search based on the search type specified in the request
     */
    public com._ach.backend.search.dto.SearchResponse search(com._ach.backend.search.dto.SearchRequest searchRequest) {
        try {
            long startTime = System.currentTimeMillis();

            com._ach.backend.search.dto.SearchResponse response;

            switch (searchRequest.getSearchType().toLowerCase()) {
                case "fuzzy":
                    response = fuzzySearch(searchRequest);
                    break;
                case "semantic":
                    response = semanticSearch(searchRequest);
                    break;
                case "hybrid":
                    response = hybridSearch(searchRequest);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid search type: " + searchRequest.getSearchType());
            }

            long took = System.currentTimeMillis() - startTime;
            response.setTook(took);

            return response;
        } catch (IOException e) {
            log.error("Error executing search", e);
            throw new RuntimeException("Search execution failed", e);
        }
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
                .index("items")
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
                        .script(sc -> sc
                                .inline(is -> is
                                        .source("cosineSimilarity(params.query_vector, 'embedding') + 1.0")
                                        .params("query_vector", jsonData -> jsonData.value(queryEmbedding))
                                )
                        )
                )
        );

        // Apply filters if provided
        Query finalQuery = applyFilters(knnQuery, searchRequest.getFilters());

        SearchRequest esRequest = SearchRequest.of(s -> s
                .index("items")
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
                        .script(sc -> sc
                                .inline(is -> is
                                        .source("cosineSimilarity(params.query_vector, 'embedding') + 1.0")
                                        .params("query_vector", jsonData -> jsonData.value(queryEmbedding))
                                )
                        )
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
                .index("items")
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
                .multiMatch(mm -> mm
                        .query(searchRequest.getQuery())
                        .fields("attributesMapId^" + searchRequest.getAttributesBoost(),
                                "searchableContent^" + searchRequest.getContentBoost())
                        .fuzziness(String.valueOf(searchRequest.getFuzziness()))
                        .prefixLength(1)
                        .maxExpansions(50)
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
                .map(hit -> new SearchHit(
                        hit.source(),
                        hit.score() != null ? hit.score().floatValue() : 0.0f,
                        searchType
                ))
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
                    .index("items")
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
                    .index("items")
                    .id(id)
            );
            log.info("Document deleted successfully: {}", id);
        } catch (IOException e) {
            log.error("Error deleting document", e);
            throw new RuntimeException("Failed to delete document", e);
        }
    }
}
