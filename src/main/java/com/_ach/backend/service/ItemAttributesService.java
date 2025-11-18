package com._ach.backend.service;

import com._ach.backend.document.ItemAttributesDocument;
import com._ach.backend.repository.ItemAttributesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemAttributesService {

    private final ItemAttributesRepository itemAttributesRepository;

    /**
     * Save or update item attributes in Elasticsearch
     */
    public String saveAttributes(Long itemId, Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            log.warn("No attributes provided for item {}", itemId);
            return null;
        }

        try {
            // Check if attributes already exist for this item
            Optional<ItemAttributesDocument> existingDoc = itemAttributesRepository.findByItemId(itemId);

            ItemAttributesDocument document;
            if (existingDoc.isPresent()) {
                // Update existing document
                document = existingDoc.get();
                document.setAttributes(attributes);
                log.info("Updating attributes for item {}", itemId);
            } else {
                // Create new document
                document = new ItemAttributesDocument();
                document.setId(UUID.randomUUID().toString());
                document.setItemId(itemId);
                document.setAttributes(attributes);
                log.info("Creating new attributes for item {}", itemId);
            }

            ItemAttributesDocument saved = itemAttributesRepository.save(document);
            log.info("Attributes saved successfully for item {} with id {}", itemId, saved.getId());
            return saved.getId();
        } catch (Exception e) {
            log.error("Error saving attributes for item {}", itemId, e);
            throw new RuntimeException("Failed to save item attributes", e);
        }
    }

    /**
     * Get attributes for an item from Elasticsearch
     */
    public Map<String, Object> getAttributes(Long itemId) {
        try {
            Optional<ItemAttributesDocument> document = itemAttributesRepository.findByItemId(itemId);
            if (document.isPresent()) {
                log.info("Attributes retrieved for item {}", itemId);
                return document.get().getAttributes();
            }
            log.warn("No attributes found for item {}", itemId);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving attributes for item {}", itemId, e);
            return null;
        }
    }

    /**
     * Get attributes by document ID
     */
    public Map<String, Object> getAttributesById(String id) {
        try {
            Optional<ItemAttributesDocument> document = itemAttributesRepository.findById(id);
            if (document.isPresent()) {
                log.info("Attributes retrieved by id {}", id);
                return document.get().getAttributes();
            }
            log.warn("No attributes found with id {}", id);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving attributes by id {}", id, e);
            return null;
        }
    }

    /**
     * Delete attributes for an item
     */
    public void deleteAttributes(Long itemId) {
        try {
            itemAttributesRepository.deleteByItemId(itemId);
            log.info("Attributes deleted for item {}", itemId);
        } catch (Exception e) {
            log.error("Error deleting attributes for item {}", itemId, e);
        }
    }

    /**
     * Delete attributes by document ID
     */
    public void deleteAttributesById(String id) {
        try {
            itemAttributesRepository.deleteById(id);
            log.info("Attributes deleted by id {}", id);
        } catch (Exception e) {
            log.error("Error deleting attributes by id {}", id, e);
        }
    }

    /**
     * Check if attributes exist for an item
     */
    public boolean attributesExist(Long itemId) {
        try {
            return itemAttributesRepository.findByItemId(itemId).isPresent();
        } catch (Exception e) {
            log.error("Error checking attributes existence for item {}", itemId, e);
            return false;
        }
    }
}
