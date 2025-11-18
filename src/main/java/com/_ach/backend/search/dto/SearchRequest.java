package com._ach.backend.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    private String query;

    // Search type: "fuzzy", "semantic", or "hybrid"
    private String searchType = "fuzzy";

    // Fuzzy search parameters
    private Integer fuzziness = 2; // AUTO, 0, 1, 2

    // Pagination
    private Integer page = 0;
    private Integer size = 10;

    // Filters
    private Map<String, Object> filters;

    // Minimum score threshold
    private Float minScore;

    // Boost parameters for different fields
    private Float attributesBoost = 1.0f;
    private Float contentBoost = 1.0f;
}
