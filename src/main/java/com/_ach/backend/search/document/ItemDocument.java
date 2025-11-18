package com._ach.backend.search.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "items")
public class ItemDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String attributesMapId;

    @Field(type = FieldType.Keyword)
    private String image;

    @Field(type = FieldType.Keyword)
    private List<String> images;

    @Field(type = FieldType.Object)
    private Map<String, Object> attributes;

    // For semantic search - stores the text content that will be used to generate embeddings
    @Field(type = FieldType.Text)
    private String searchableContent;

    // Dense vector for semantic search (768 dimensions for typical BERT-based models)
    @Field(type = FieldType.Dense_Vector, dims = 768)
    private float[] embedding;

    // Timestamp fields
    @Field(type = FieldType.Date)
    private Long createdAt;

    @Field(type = FieldType.Date)
    private Long updatedAt;
}
