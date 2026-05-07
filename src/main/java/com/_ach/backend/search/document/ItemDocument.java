package com._ach.backend.search.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDocument {

    @Id
    private String id;

    private String attributesMapId;

    private String image;

    private List<String> images;

    private Map<String, Object> attributes;

    // For semantic search - stores the text content that will be used to generate embeddings
    private String searchableContent;

    // Dense vector for semantic search (768 dimensions for typical BERT-based models)
    private float[] embedding;

    // Timestamp fields
    private Long createdAt;

    private Long updatedAt;
}
