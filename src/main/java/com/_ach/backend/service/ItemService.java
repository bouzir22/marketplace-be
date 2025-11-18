package com._ach.backend.service;

import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.document.AttributesMap;
import com._ach.backend.entity.Item;
import com._ach.backend.repository.ItemRepository;
import com._ach.backend.exception.ResourceNotFoundException;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ItemService {
    private final ItemRepository itemRepository;
    private final AttributesMapService attributesMapService;

    public ItemService(ItemRepository itemRepository, AttributesMapService attributesMapService) {
        this.itemRepository = itemRepository;
        this.attributesMapService = attributesMapService;
    }

    // CREATE - Create a new item
    public ItemRepresentation createItem(ItemRepresentation itemRepresentation) {
        // Step 1: Save attributes to Elasticsearch first
        String attributesMapId = attributesMapService.saveAttributesMap(itemRepresentation.getAttributes());

        // Step 2: Create Item entity with the Elasticsearch ID
        Item item = new Item();
        item.setAttributesMapId(attributesMapId);
        item.setImage(itemRepresentation.getImage());
        item.setImages(itemRepresentation.getImages());

        // Step 3: Save to PostgreSQL
        Item savedItem = itemRepository.save(item);

        // Step 4: Return representation
        return toRepresentation(savedItem);
    }

    // READ - Filter items with Querydsl predicate
    public List<ItemRepresentation> filterItems(Predicate predicate) {
        List<Item> items = (List<Item>) itemRepository.findAll(predicate);
        return items.stream().map(this::toRepresentation).toList();
    }

    // READ - Get item by ID
    public ItemRepresentation getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return toRepresentation(item);
    }

    // UPDATE - Full update of an item
    public ItemRepresentation updateItem(Long id, ItemRepresentation itemRepresentation) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        // Update attributes in Elasticsearch
        String attributesMapId = attributesMapService.updateAttributesMap(
                item.getAttributesMapId(),
                itemRepresentation.getAttributes()
        );

        // Update item fields
        item.setAttributesMapId(attributesMapId);
        item.setImage(itemRepresentation.getImage());
        item.setImages(itemRepresentation.getImages());

        Item savedItem = itemRepository.save(item);
        return toRepresentation(savedItem);
    }

    // UPDATE - Partial update of an item
    public ItemRepresentation partialUpdateItem(Long id, ItemRepresentation itemRepresentation) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        // Update attributes in Elasticsearch if provided
        if (itemRepresentation.getAttributes() != null) {
            String attributesMapId = attributesMapService.updateAttributesMap(
                    item.getAttributesMapId(),
                    itemRepresentation.getAttributes()
            );
            item.setAttributesMapId(attributesMapId);
        }

        // Update only non-null fields
        if (itemRepresentation.getImage() != null) {
            item.setImage(itemRepresentation.getImage());
        }
        if (itemRepresentation.getImages() != null) {
            item.setImages(itemRepresentation.getImages());
        }

        Item savedItem = itemRepository.save(item);
        return toRepresentation(savedItem);
    }

    // DELETE - Delete a single item
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        // Delete from Elasticsearch first
        if (item.getAttributesMapId() != null) {
            attributesMapService.deleteAttributesMap(item.getAttributesMapId());
        }

        // Delete from PostgreSQL
        itemRepository.delete(item);
    }

    // DELETE - Delete multiple items
    public void deleteItems(List<Long> ids) {
        List<Item> items = itemRepository.findAllById(ids);

        // Delete attributes from Elasticsearch
        items.forEach(item -> {
            if (item.getAttributesMapId() != null) {
                attributesMapService.deleteAttributesMap(item.getAttributesMapId());
            }
        });

        // Delete from PostgreSQL
        itemRepository.deleteAllById(ids);
    }

    // Helper method to convert Item to ItemRepresentation
    private ItemRepresentation toRepresentation(Item item) {
        ItemRepresentation representation = new ItemRepresentation();
        representation.setId(item.getId());
        representation.setImage(item.getImage());
        representation.setImages(item.getImages());

        // Fetch attributes from Elasticsearch
        if (item.getAttributesMapId() != null) {
            AttributesMap attributesMap = attributesMapService.getAttributesMap(item.getAttributesMapId());
            if (attributesMap != null) {
                representation.setAttributes(attributesMap.getAttributes());
            }
        }

        return representation;
    }
}