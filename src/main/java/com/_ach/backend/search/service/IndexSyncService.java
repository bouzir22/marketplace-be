package com._ach.backend.search.service;

import com._ach.backend.entity.Item;
import com._ach.backend.repository.ItemRepository;
import com._ach.backend.search.document.ItemDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to synchronize items from PostgreSQL database to Elasticsearch index
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IndexSyncService {

    private final ItemRepository itemRepository;
    private final SearchService searchService;

    /**
     * Syncs all items from the database to Elasticsearch
     *
     * @return number of items synced
     */
    public long syncAllItems() {
        log.info("Starting full sync of items to Elasticsearch");

        List<Item> items = itemRepository.findAll();
        long count = 0;

        for (Item item : items) {
            try {
                ItemDocument document = convertToDocument(item);
                searchService.indexDocument(document);
                count++;
            } catch (Exception e) {
                log.error("Failed to index item with id: {}", item.getId(), e);
            }
        }

        log.info("Completed sync. Total items indexed: {}", count);
        return count;
    }

    /**
     * Syncs a single item to Elasticsearch
     *
     * @param itemId the ID of the item to sync
     */
    public void syncItem(Long itemId) {
        log.info("Syncing item {} to Elasticsearch", itemId);

        itemRepository.findById(itemId).ifPresentOrElse(
                item -> {
                    ItemDocument document = convertToDocument(item);
                    searchService.indexDocument(document);
                    log.info("Item {} synced successfully", itemId);
                },
                () -> log.warn("Item {} not found in database", itemId)
        );
    }

    /**
     * Removes an item from Elasticsearch
     *
     * @param itemId the ID of the item to remove
     */
    public void removeItem(Long itemId) {
        log.info("Removing item {} from Elasticsearch", itemId);
        searchService.deleteDocument(String.valueOf(itemId));
    }

    /**
     * Converts a JPA Item entity to an Elasticsearch ItemDocument
     */
    private ItemDocument convertToDocument(Item item) {
        ItemDocument document = new ItemDocument();
        document.setId(String.valueOf(item.getId()));
        document.setAttributesMapId(item.getAttributesMapId());
        document.setImage(item.getImage());
        document.setImages(item.getImages());

        // Build searchable content from available fields
        StringBuilder searchableContent = new StringBuilder();
        if (item.getAttributesMapId() != null) {
            searchableContent.append(item.getAttributesMapId()).append(" ");
        }

        // You can add more fields to searchable content as needed
        // For example, if you parse the attributes, you can add them here

        document.setSearchableContent(searchableContent.toString().trim());

        // Note: attributes map would need to be populated if you have attribute data
        // This might require additional logic to fetch and parse attribute data

        return document;
    }
}
