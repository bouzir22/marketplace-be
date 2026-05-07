package com._ach.backend.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetResponseItem;
import com._ach.backend.document.ItemDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemDetailsService {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${elasticsearch.index.name}")
    private String indexName;

    @Autowired
    public ItemDetailsService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * Saves item details to Elasticsearch and returns the generated ID
     * @param attributes The map of item details to save
     * @return The Elasticsearch-generated document ID
     */
    public String saveItemDetails(Map<String, Object> attributes) {
        ItemDetails itemDetails = new ItemDetails();
        itemDetails.setAttributes(attributes);

        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                    .index(indexName)
                    .document(itemDetails)
            );
            return response.id();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save item details", e);
        }
    }

    /**
     * Updates existing item details in Elasticsearch
     * @param id The Elasticsearch document ID
     * @param attributes The new item details map
     * @return The updated Elasticsearch document ID
     */
    public String updateItemDetails(String id, Map<String, Object> attributes) {
        ItemDetails itemDetails = new ItemDetails();
        itemDetails.setId(id);
        itemDetails.setAttributes(attributes);

        try {
            elasticsearchClient.index(i -> i
                    .index(indexName)
                    .id(id)
                    .document(itemDetails)
            );
            return id;
        } catch (IOException e) {
            throw new RuntimeException("Failed to update item details", e);
        }
    }

    /**
     * Retrieves item details from Elasticsearch
     * @param id The Elasticsearch document ID
     * @return The ItemDetails, or null if not found
     */
    public ItemDetails getItemDetails(String id) {
        try {
            GetResponse<ItemDetails> response = elasticsearchClient.get(g -> g
                    .index(indexName)
                    .id(id),
                    ItemDetails.class
            );
            return response.found() ? response.source() : null;
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve item details", e);
        }
    }

    /**
     * Deletes item details from Elasticsearch
     * @param id The Elasticsearch document ID
     */
    public void deleteItemDetails(String id) {
        try {
            elasticsearchClient.delete(d -> d
                    .index(indexName)
                    .id(id)
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete item details", e);
        }
    }

    /**
     * Batch retrieves item details from Elasticsearch using mget
     * @param ids List of Elasticsearch document IDs
     * @return Map of document ID to ItemDetails
     */
    public Map<String, ItemDetails> getItemDetailsBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashMap<>();
        }

        try {
            MgetResponse<ItemDetails> response = elasticsearchClient.mget(m -> m
                    .index(indexName)
                    .ids(ids),
                    ItemDetails.class
            );

            Map<String, ItemDetails> result = new HashMap<>();
            for (MultiGetResponseItem<ItemDetails> item : response.docs()) {
                if (item.isResult() && item.result().found()) {
                    result.put(item.result().id(), item.result().source());
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to batch retrieve item details", e);
        }
    }
}
