package com._ach.backend.repository;

import com._ach.backend.document.ItemDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch repository for managing item attributes in Elasticsearch.
 * Provides search capabilities across all item attributes.
 */
@Repository
public interface ItemDocumentRepository extends ElasticsearchRepository<ItemDocument, String> {

    /**
     * Find items by specific attribute value
     * Example: findByAttributesContaining("category", "electronics")
     */
    List<ItemDocument> findByAttributes(String key, Object value);
}
