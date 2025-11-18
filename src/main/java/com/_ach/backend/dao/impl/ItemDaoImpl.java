package com._ach.backend.dao.impl;

import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.dao.ItemDao;
import com._ach.backend.entity.Item;
import com._ach.backend.repository.ItemRepository;
import com._ach.backend.service.ImageStorageService;
import com._ach.backend.service.ItemAttributesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ItemDaoImpl implements ItemDao {

    private final ItemRepository itemRepository;
    private final ItemAttributesService itemAttributesService;
    private final ImageStorageService imageStorageService;

    @Override
    public ItemRepresentation create(ItemRepresentation itemRepresentation) {
        log.info("Creating new item");

        // Create new Item entity
        Item item = new Item();
        item.setImage(itemRepresentation.getImage());
        item.setImages(itemRepresentation.getImages());

        // Save item to database first to get the ID
        Item savedItem = itemRepository.save(item);
        log.info("Item saved to database with id: {}", savedItem.getId());

        // Save attributes to Elasticsearch if present
        if (itemRepresentation.getAttributes() != null && !itemRepresentation.getAttributes().isEmpty()) {
            String attributesId = itemAttributesService.saveAttributes(
                savedItem.getId(),
                itemRepresentation.getAttributes()
            );
            savedItem.setAttributesMapId(attributesId);
            savedItem = itemRepository.save(savedItem);
            log.info("Attributes saved to Elasticsearch with id: {}", attributesId);
        }

        return mapToRepresentation(savedItem);
    }

    @Override
    public Optional<ItemRepresentation> findById(Long id) {
        log.info("Finding item by id: {}", id);

        Optional<Item> itemOptional = itemRepository.findById(id);

        return itemOptional.map(this::mapToRepresentation);
    }

    @Override
    public List<ItemRepresentation> findAll(Pageable pageable, Map<String, String> filter) {
        log.info("Finding all items with pagination and filters");

        // For now, simple implementation without filters
        // TODO: Implement filtering using QueryDSL predicates
        Page<Item> itemPage = itemRepository.findAll(pageable);

        return itemPage.getContent().stream()
            .map(this::mapToRepresentation)
            .collect(Collectors.toList());
    }

    @Override
    public ItemRepresentation update(ItemRepresentation itemRepresentation) {
        log.info("Updating item with id: {}", itemRepresentation.getId());

        if (!itemRepository.existsById(itemRepresentation.getId())) {
            throw new IllegalArgumentException("Item with id " + itemRepresentation.getId() + " does not exist");
        }

        // Find existing item
        Item existingItem = itemRepository.findById(itemRepresentation.getId())
            .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        // Update basic fields
        existingItem.setImage(itemRepresentation.getImage());
        existingItem.setImages(itemRepresentation.getImages());

        // Update or create attributes in Elasticsearch
        if (itemRepresentation.getAttributes() != null && !itemRepresentation.getAttributes().isEmpty()) {
            String attributesId = itemAttributesService.saveAttributes(
                existingItem.getId(),
                itemRepresentation.getAttributes()
            );
            existingItem.setAttributesMapId(attributesId);
            log.info("Attributes updated in Elasticsearch");
        }

        // Save updated item
        Item updatedItem = itemRepository.save(existingItem);
        log.info("Item updated successfully with id: {}", updatedItem.getId());

        return mapToRepresentation(updatedItem);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting item with id: {}", id);

        // Find item to get image URLs and attributes ID for cleanup
        Optional<Item> itemOptional = itemRepository.findById(id);

        if (itemOptional.isPresent()) {
            Item item = itemOptional.get();

            // Delete images from S3
            if (item.getImage() != null && !item.getImage().isEmpty()) {
                try {
                    imageStorageService.deleteImage(item.getImage());
                    log.info("Main image deleted from storage");
                } catch (Exception e) {
                    log.warn("Failed to delete main image: {}", e.getMessage());
                }
            }

            if (item.getImages() != null && !item.getImages().isEmpty()) {
                try {
                    imageStorageService.deleteImages(item.getImages());
                    log.info("Additional images deleted from storage");
                } catch (Exception e) {
                    log.warn("Failed to delete additional images: {}", e.getMessage());
                }
            }

            // Delete attributes from Elasticsearch
            if (item.getAttributesMapId() != null && !item.getAttributesMapId().isEmpty()) {
                try {
                    itemAttributesService.deleteAttributesById(item.getAttributesMapId());
                    log.info("Attributes deleted from Elasticsearch");
                } catch (Exception e) {
                    log.warn("Failed to delete attributes: {}", e.getMessage());
                }
            }
        }

        // Delete item from database
        itemRepository.deleteById(id);
        log.info("Item deleted from database with id: {}", id);
    }

    @Override
    public boolean exists(Long id) {
        return itemRepository.existsById(id);
    }

    @Override
    public long count() {
        return itemRepository.count();
    }

    /**
     * Map Item entity to ItemRepresentation
     */
    private ItemRepresentation mapToRepresentation(Item item) {
        ItemRepresentation representation = new ItemRepresentation();
        representation.setId(item.getId());
        representation.setImage(item.getImage());
        representation.setImages(item.getImages());

        // Fetch attributes from Elasticsearch if attributesMapId is present
        if (item.getAttributesMapId() != null && !item.getAttributesMapId().isEmpty()) {
            Map<String, Object> attributes = itemAttributesService.getAttributesById(item.getAttributesMapId());
            representation.setAttributes(attributes);
        }

        return representation;
    }
}
