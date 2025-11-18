package com._ach.backend.service;

import com._ach.backend.document.ItemDocument;
import com._ach.backend.repository.ItemDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing item attributes in Elasticsearch.
 * Handles synchronization of attributes between the application and Elasticsearch.
 */
@Service
@RequiredArgsConstructor
public class ElasticsearchService {

    private final ItemDocumentRepository itemDocumentRepository;

    /**
     * Save or update item attributes in Elasticsearch
     *
     * @param itemId     The item ID from the database
     * @param attributes Map of item attributes
     * @return The saved ItemDocument
     */
    public ItemDocument saveItemAttributes(Long itemId, Map<String, Object> attributes) {
        ItemDocument document = new ItemDocument(itemId.toString(), attributes);
        return itemDocumentRepository.save(document);
    }

    /**
     * Get item attributes from Elasticsearch
     *
     * @param itemId The item ID
     * @return Optional containing the ItemDocument if found
     */
    public Optional<ItemDocument> getItemAttributes(Long itemId) {
        return itemDocumentRepository.findById(itemId.toString());
    }

    /**
     * Delete item attributes from Elasticsearch
     *
     * @param itemId The item ID to delete
     */
    public void deleteItemAttributes(Long itemId) {
        itemDocumentRepository.deleteById(itemId.toString());
    }

    /**
     * Delete multiple items from Elasticsearch
     *
     * @param itemIds List of item IDs to delete
     */
    public void deleteItemAttributes(List<Long> itemIds) {
        itemIds.forEach(id -> itemDocumentRepository.deleteById(id.toString()));
    }

    /**
     * Search items by any attribute
     *
     * @param key   Attribute key
     * @param value Attribute value
     * @return List of matching ItemDocuments
     */
    public List<ItemDocument> searchByAttribute(String key, Object value) {
        return itemDocumentRepository.findByAttributes(key, value);
    }

    /**
     * Get all items from Elasticsearch
     *
     * @return Iterable of all ItemDocuments
     */
    public Iterable<ItemDocument> getAllItems() {
        return itemDocumentRepository.findAll();
    }

    /**
     * Update specific attributes for an item
     *
     * @param itemId    The item ID
     * @param attributes Map of attributes to update/add
     * @return The updated ItemDocument
     */
    public Optional<ItemDocument> updateItemAttributes(Long itemId, Map<String, Object> attributes) {
        Optional<ItemDocument> existingDoc = getItemAttributes(itemId);

        if (existingDoc.isPresent()) {
            ItemDocument document = existingDoc.get();
            Map<String, Object> currentAttributes = document.getAttributes();

            // Merge new attributes with existing ones
            if (currentAttributes != null) {
                currentAttributes.putAll(attributes);
            } else {
                document.setAttributes(attributes);
            }

            return Optional.of(itemDocumentRepository.save(document));
        }

        return Optional.empty();
    }
}
