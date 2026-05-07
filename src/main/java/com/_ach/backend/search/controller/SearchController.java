package com._ach.backend.search.controller;

import com._ach.backend.search.document.ItemDocument;
import com._ach.backend.search.dto.ItemSearchResponse;
import com._ach.backend.search.dto.SearchRequest;
import com._ach.backend.search.dto.SearchResponse;
import com._ach.backend.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Elasticsearch search operations with fuzzy and semantic search")
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    @Operation(
            summary = "Search items",
            description = "Search items using fuzzy search, semantic search, or hybrid approach"
    )
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Simple search",
            description = "Search by attribute key-value pairs. Use any attribute as a query param (e.g., category=Cheese, name=Gouda)"
    )
    public ResponseEntity<ItemSearchResponse> simpleSearch(
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") Integer size,

            @RequestParam Map<String, String> allParams
    ) {
        // Remove pagination params to get only attribute filters
        Map<String, Object> filters = new HashMap<>();
        allParams.forEach((key, value) -> {
            if (!key.equals("page") && !key.equals("size")) {
                filters.put(key, value);
            }
        });

        ItemSearchResponse response = searchService.searchByAttributes(filters, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/index")
    @Operation(
            summary = "Index a document",
            description = "Index or update a document in Elasticsearch"
    )
    public ResponseEntity<String> indexDocument(@RequestBody ItemDocument document) {
        searchService.indexDocument(document);
        return ResponseEntity.ok("Document indexed successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a document",
            description = "Delete a document from Elasticsearch by ID"
    )
    public ResponseEntity<String> deleteDocument(@PathVariable String id) {
        searchService.deleteDocument(id);
        return ResponseEntity.ok("Document deleted successfully");
    }
}
