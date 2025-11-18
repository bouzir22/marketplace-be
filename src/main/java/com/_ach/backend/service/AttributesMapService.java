package com._ach.backend.service;

import com._ach.backend.document.AttributesMap;
import com._ach.backend.repository.AttributesMapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AttributesMapService {

    @Autowired
    private AttributesMapRepository attributesMapRepository;

    /**
     * Saves an attributes map to Elasticsearch and returns the generated ID
     * @param attributes The map of attributes to save
     * @return The Elasticsearch document ID
     */
    public String saveAttributesMap(Map<String, Object> attributes) {
        AttributesMap attributesMap = new AttributesMap();
        attributesMap.setAttributes(attributes);

        AttributesMap saved = attributesMapRepository.save(attributesMap);
        return saved.getId();
    }

    /**
     * Updates an existing attributes map in Elasticsearch
     * @param id The Elasticsearch document ID
     * @param attributes The new attributes map
     * @return The updated Elasticsearch document ID
     */
    public String updateAttributesMap(String id, Map<String, Object> attributes) {
        AttributesMap attributesMap = new AttributesMap();
        attributesMap.setId(id);
        attributesMap.setAttributes(attributes);

        AttributesMap saved = attributesMapRepository.save(attributesMap);
        return saved.getId();
    }

    /**
     * Retrieves an attributes map from Elasticsearch
     * @param id The Elasticsearch document ID
     * @return The attributes map, or null if not found
     */
    public AttributesMap getAttributesMap(String id) {
        return attributesMapRepository.findById(id).orElse(null);
    }

    /**
     * Deletes an attributes map from Elasticsearch
     * @param id The Elasticsearch document ID
     */
    public void deleteAttributesMap(String id) {
        attributesMapRepository.deleteById(id);
    }
}
