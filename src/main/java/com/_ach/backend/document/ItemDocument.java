package com._ach.backend.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Map;

/**
 * Elasticsearch document for storing item attributes.
 * All product attributes (except ID and image URLs) are stored here for efficient searching.
 */
@Document(indexName = "items")
@Getter
@Setter
public class ItemDocument {

    @Id
    private String id;  // Same as database Item.id (stored as String in ES)

    /**
     * Dynamic attributes map that can store any product-specific attributes
     * Examples: name, brand, description, price, category, size, color, etc.
     */
    @Field(type = FieldType.Object)
    private Map<String, Object> attributes;

    /**
     * Constructor
     */
    public ItemDocument() {
    }

    public ItemDocument(String id, Map<String, Object> attributes) {
        this.id = id;
        this.attributes = attributes;
    }
}
