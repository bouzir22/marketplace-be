package com._ach.backend.service;

import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.document.ItemDocument;
import com._ach.backend.entity.Item;
import com._ach.backend.repository.ItemRepository;
import com._ach.backend.exception.ResourceNotFoundException;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing generic items.
 * Coordinates between:
 * - Database (PostgreSQL) for ID and image URLs
 * - Elasticsearch for all other attributes
 * - Remote storage (S3) for actual images
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final ElasticsearchService elasticsearchService;
    private final ImageStorageService imageStorageService;

    /**
     * Create a new item with attributes and images
     *
     * @param mainImage Main image file
     * @param additionalImages Additional image files
     * @param attributes Map of item attributes (name, price, etc.)
     * @return The created ItemRepresentation
     */
    public ItemRepresentation createItem(
            MultipartFile mainImage,
            List<MultipartFile> additionalImages,
            Map<String, Object> attributes) throws IOException {

        // 1. Upload images to remote storage and get URLs
        String mainImageUrl = null;
        if (mainImage != null && !mainImage.isEmpty()) {
            mainImageUrl = imageStorageService.uploadImage(mainImage);
        }

        List<String> additionalImageUrls = new ArrayList<>();
        if (additionalImages != null && !additionalImages.isEmpty()) {
            additionalImageUrls = imageStorageService.uploadImages(additionalImages);
        }

        // 2. Create and save item in database (only ID and image URLs)
        Item item = new Item();
        item.setImage(mainImageUrl);
        item.setImages(additionalImageUrls);
        item.setAttributesMapId(UUID.randomUUID().toString()); // Generate unique ID for attribute mapping

        item = itemRepository.save(item);

        // 3. Save attributes to Elasticsearch
        if (attributes != null && !attributes.isEmpty()) {
            elasticsearchService.saveItemAttributes(item.getId(), attributes);
        }

        // 4. Return combined representation
        return buildItemRepresentation(item, attributes);
    }

    /**
     * Create item from ItemRepresentation (for backward compatibility)
     *
     * @param itemRep ItemRepresentation with attributes
     * @return The created ItemRepresentation
     */
    public ItemRepresentation createItemFromRepresentation(ItemRepresentation itemRep) {
        // Save to database
        Item item = new Item();
        item.setImage(itemRep.getImage());
        item.setImages(itemRep.getImages());
        item.setAttributesMapId(UUID.randomUUID().toString());

        item = itemRepository.save(item);

        // Save attributes to Elasticsearch
        if (itemRep.getAttributes() != null && !itemRep.getAttributes().isEmpty()) {
            elasticsearchService.saveItemAttributes(item.getId(), itemRep.getAttributes());
        }

        return buildItemRepresentation(item, itemRep.getAttributes());
    }

    /**
     * Get all items with their attributes
     *
     * @return List of ItemRepresentations
     */
    public List<ItemRepresentation> getAllItems() {
        List<Item> items = itemRepository.findAll();
        return items.stream()
                .map(this::getItemRepresentation)
                .collect(Collectors.toList());
    }

    /**
     * Filter items with QueryDSL predicate
     * Note: This only filters on database fields (id, image URLs)
     * For attribute-based filtering, use searchByAttribute
     *
     * @param predicate QueryDSL predicate
     * @return List of ItemRepresentations
     */
    public List<ItemRepresentation> filterItems(Predicate predicate) {
        List<Item> items = (List<Item>) itemRepository.findAll(predicate);
        return items.stream()
                .map(this::getItemRepresentation)
                .collect(Collectors.toList());
    }

    /**
     * Get item by ID with attributes
     *
     * @param id Item ID
     * @return ItemRepresentation
     */
    public ItemRepresentation getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        return getItemRepresentation(item);
    }

    /**
     * Update item with new images and attributes
     *
     * @param id Item ID
     * @param mainImage New main image (optional)
     * @param additionalImages New additional images (optional)
     * @param attributes New attributes
     * @return Updated ItemRepresentation
     */
    public ItemRepresentation updateItem(
            Long id,
            MultipartFile mainImage,
            List<MultipartFile> additionalImages,
            Map<String, Object> attributes) throws IOException {

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        // Update main image if provided
        if (mainImage != null && !mainImage.isEmpty()) {
            // Delete old image
            if (item.getImage() != null) {
                imageStorageService.deleteImage(item.getImage());
            }
            // Upload new image
            String newImageUrl = imageStorageService.uploadImage(mainImage);
            item.setImage(newImageUrl);
        }

        // Update additional images if provided
        if (additionalImages != null && !additionalImages.isEmpty()) {
            // Delete old images
            if (item.getImages() != null) {
                imageStorageService.deleteImages(item.getImages());
            }
            // Upload new images
            List<String> newImageUrls = imageStorageService.uploadImages(additionalImages);
            item.setImages(newImageUrls);
        }

        // Save database changes
        item = itemRepository.save(item);

        // Update attributes in Elasticsearch
        if (attributes != null && !attributes.isEmpty()) {
            elasticsearchService.saveItemAttributes(id, attributes);
        }

        return getItemRepresentation(item);
    }

    /**
     * Partial update of item attributes only (no images)
     *
     * @param id Item ID
     * @param attributes Attributes to update/add
     * @return Updated ItemRepresentation
     */
    public ItemRepresentation partialUpdateAttributes(Long id, Map<String, Object> attributes) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (attributes != null && !attributes.isEmpty()) {
            elasticsearchService.updateItemAttributes(id, attributes);
        }

        return getItemRepresentation(item);
    }

    /**
     * Delete a single item (from database, Elasticsearch, and remote storage)
     *
     * @param id Item ID
     */
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        // Delete images from remote storage
        if (item.getImage() != null) {
            imageStorageService.deleteImage(item.getImage());
        }
        if (item.getImages() != null) {
            imageStorageService.deleteImages(item.getImages());
        }

        // Delete from Elasticsearch
        elasticsearchService.deleteItemAttributes(id);

        // Delete from database
        itemRepository.delete(item);
    }

    /**
     * Delete multiple items
     *
     * @param ids List of item IDs
     */
    public void deleteItems(List<Long> ids) {
        List<Item> items = itemRepository.findAllById(ids);

        // Delete all images
        for (Item item : items) {
            if (item.getImage() != null) {
                imageStorageService.deleteImage(item.getImage());
            }
            if (item.getImages() != null) {
                imageStorageService.deleteImages(item.getImages());
            }
        }

        // Delete from Elasticsearch
        elasticsearchService.deleteItemAttributes(ids);

        // Delete from database
        itemRepository.deleteAllById(ids);
    }

    /**
     * Search items by attribute in Elasticsearch
     *
     * @param attributeKey Attribute key (e.g., "category", "brand")
     * @param attributeValue Attribute value
     * @return List of ItemRepresentations
     */
    public List<ItemRepresentation> searchByAttribute(String attributeKey, Object attributeValue) {
        List<ItemDocument> documents = elasticsearchService.searchByAttribute(attributeKey, attributeValue);

        return documents.stream()
                .map(doc -> {
                    Long itemId = Long.parseLong(doc.getId());
                    Optional<Item> itemOpt = itemRepository.findById(itemId);
                    return itemOpt.map(item -> buildItemRepresentation(item, doc.getAttributes()))
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Build ItemRepresentation from Item entity
     *
     * @param item Item entity
     * @return ItemRepresentation
     */
    private ItemRepresentation getItemRepresentation(Item item) {
        // Get attributes from Elasticsearch
        Optional<ItemDocument> docOpt = elasticsearchService.getItemAttributes(item.getId());
        Map<String, Object> attributes = docOpt.map(ItemDocument::getAttributes).orElse(new HashMap<>());

        return buildItemRepresentation(item, attributes);
    }

    /**
     * Build ItemRepresentation from Item and attributes
     *
     * @param item Item entity
     * @param attributes Attributes map
     * @return ItemRepresentation
     */
    private ItemRepresentation buildItemRepresentation(Item item, Map<String, Object> attributes) {
        ItemRepresentation rep = new ItemRepresentation();
        rep.setId(item.getId());
        rep.setImage(item.getImage());
        rep.setImages(item.getImages());
        rep.setAttributes(attributes != null ? attributes : new HashMap<>());
        return rep;
    }
}
