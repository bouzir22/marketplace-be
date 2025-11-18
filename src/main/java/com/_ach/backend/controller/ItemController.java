package com._ach.backend.controller;

import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.entity.Item;
import com._ach.backend.service.ItemService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing generic items.
 * Supports:
 * - Image uploads (multipart/form-data)
 * - Dynamic attributes storage
 * - Search and filtering
 */
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final ObjectMapper objectMapper;

    /**
     * Create a new item with images and attributes
     * Accepts multipart/form-data with:
     * - mainImage: Main product image
     * - additionalImages: Additional product images (multiple files)
     * - attributes: JSON string with item attributes
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ItemRepresentation> createItem(
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages,
            @RequestPart(value = "attributes") String attributesJson) throws IOException {

        // Parse attributes JSON
        Map<String, Object> attributes = objectMapper.readValue(
                attributesJson,
                new TypeReference<Map<String, Object>>() {}
        );

        ItemRepresentation createdItem = itemService.createItem(mainImage, additionalImages, attributes);
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    /**
     * Create item from JSON (for backward compatibility, no image upload)
     * Accepts application/json with ItemRepresentation
     */
    @PostMapping(consumes = "application/json")
    public ResponseEntity<ItemRepresentation> createItemFromJson(@RequestBody ItemRepresentation itemRep) {
        ItemRepresentation createdItem = itemService.createItemFromRepresentation(itemRep);
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    /**
     * Get all items with their attributes
     */
    @GetMapping("/all")
    public ResponseEntity<List<ItemRepresentation>> getAllItems() {
        List<ItemRepresentation> items = itemService.getAllItems();
        return ResponseEntity.ok(items);
    }

    /**
     * Filter items with QueryDSL predicate
     * Note: This filters only on database fields (id, images)
     * For attribute-based search, use /items/search
     */
    @GetMapping
    public ResponseEntity<List<ItemRepresentation>> filterItems(
            @QuerydslPredicate(root = Item.class) Predicate predicate) {
        List<ItemRepresentation> items = itemService.filterItems(predicate);
        return ResponseEntity.ok(items);
    }

    /**
     * Get item by ID with all attributes
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItemRepresentation> getItemById(@PathVariable Long id) {
        ItemRepresentation item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    /**
     * Search items by attribute
     * Example: GET /items/search?key=category&value=electronics
     */
    @GetMapping("/search")
    public ResponseEntity<List<ItemRepresentation>> searchByAttribute(
            @RequestParam String key,
            @RequestParam String value) {
        List<ItemRepresentation> items = itemService.searchByAttribute(key, value);
        return ResponseEntity.ok(items);
    }

    /**
     * Update item with new images and/or attributes
     * Accepts multipart/form-data
     */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ItemRepresentation> updateItem(
            @PathVariable Long id,
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages,
            @RequestPart(value = "attributes", required = false) String attributesJson) throws IOException {

        Map<String, Object> attributes = null;
        if (attributesJson != null && !attributesJson.isEmpty()) {
            attributes = objectMapper.readValue(
                    attributesJson,
                    new TypeReference<Map<String, Object>>() {}
            );
        }

        ItemRepresentation updatedItem = itemService.updateItem(id, mainImage, additionalImages, attributes);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * Partial update of item attributes only (no images)
     * Accepts application/json
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ItemRepresentation> partialUpdateAttributes(
            @PathVariable Long id,
            @RequestBody Map<String, Object> attributes) {
        ItemRepresentation updatedItem = itemService.partialUpdateAttributes(id, attributes);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * Delete a single item (including images and attributes)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete multiple items
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteItems(@RequestBody List<Long> ids) {
        itemService.deleteItems(ids);
        return ResponseEntity.noContent().build();
    }
}
