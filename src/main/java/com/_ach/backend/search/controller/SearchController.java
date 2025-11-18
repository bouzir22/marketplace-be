package com._ach.backend.search.controller;

import com._ach.backend.search.document.ItemDocument;
import com._ach.backend.search.dto.SearchRequest;
import com._ach.backend.search.dto.SearchResponse;
import com._ach.backend.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            description = "Simple search with query parameter (fuzzy search by default)"
    )
    public ResponseEntity<SearchResponse> simpleSearch(
            @Parameter(description = "Search query")
            @RequestParam String query,

            @Parameter(description = "Search type: fuzzy, semantic, or hybrid")
            @RequestParam(defaultValue = "fuzzy") String searchType,

            @Parameter(description = "Fuzziness level (0-2)")
            @RequestParam(defaultValue = "2") Integer fuzziness,

            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") Integer size,

            @Parameter(description = "Minimum score threshold")
            @RequestParam(required = false) Float minScore
    ) {
        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setSearchType(searchType);
        request.setFuzziness(fuzziness);
        request.setPage(page);
        request.setSize(size);
        request.setMinScore(minScore);

        SearchResponse response = searchService.search(request);
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
