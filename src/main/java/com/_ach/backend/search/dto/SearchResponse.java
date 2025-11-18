package com._ach.backend.search.dto;

import com._ach.backend.search.document.ItemDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private List<SearchHit> hits;
    private Long totalHits;
    private Integer page;
    private Integer size;
    private Long took; // Time taken in milliseconds

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchHit {
        private ItemDocument document;
        private Float score;
        private String searchType; // "fuzzy" or "semantic"
    }
}
