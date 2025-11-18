package com._ach.backend.controller;

import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.service.ItemService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;
    private final ObjectMapper objectMapper;

    // CREATE - Create a new item (JSON only)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ItemRepresentation> createItem(@RequestBody ItemRepresentation item) {
        log.info("Creating item via JSON");
        ItemRepresentation createdItem = itemService.createItem(item);
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    // CREATE - Create item with file uploads
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemRepresentation> createItemWithFiles(
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages,
            @RequestPart(value = "attributes", required = false) String attributesJson) throws IOException {

        log.info("Creating item with file uploads");

        // Parse attributes JSON
        Map<String, Object> attributes = new HashMap<>();
        if (attributesJson != null && !attributesJson.isEmpty()) {
            attributes = objectMapper.readValue(attributesJson, new TypeReference<Map<String, Object>>() {});
        }

        ItemRepresentation createdItem = itemService.createItemWithImages(
                mainImage,
                additionalImages,
                attributes
        );

        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    // READ - Get all items with pagination
    @GetMapping
    public ResponseEntity<List<ItemRepresentation>> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Map<String, String> filters) {

        log.info("Getting all items - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<ItemRepresentation> items = itemService.getAllItems(pageable, filters);

        return ResponseEntity.ok(items);
    }

    // READ - Get item by ID
    @GetMapping("/{id}")
    public ResponseEntity<ItemRepresentation> getItemById(@PathVariable Long id) {
        log.info("Getting item by id: {}", id);
        ItemRepresentation item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    // UPDATE - Update an existing item (JSON only)
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ItemRepresentation> updateItem(
            @PathVariable Long id,
            @RequestBody ItemRepresentation item) {

        log.info("Updating item {} via JSON", id);
        ItemRepresentation updatedItem = itemService.updateItem(id, item);
        return ResponseEntity.ok(updatedItem);
    }

    // UPDATE - Update item with file uploads
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemRepresentation> updateItemWithFiles(
            @PathVariable Long id,
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestPart(value = "additionalImages", required = false) List<MultipartFile> additionalImages,
            @RequestPart(value = "attributes", required = false) String attributesJson) throws IOException {

        log.info("Updating item {} with file uploads", id);

        // Parse attributes JSON
        Map<String, Object> attributes = new HashMap<>();
        if (attributesJson != null && !attributesJson.isEmpty()) {
            attributes = objectMapper.readValue(attributesJson, new TypeReference<Map<String, Object>>() {});
        }

        ItemRepresentation updatedItem = itemService.updateItemWithImages(
                id,
                mainImage,
                additionalImages,
                attributes
        );

        return ResponseEntity.ok(updatedItem);
    }

    // UPDATE - Partial update (PATCH)
    @PatchMapping("/{id}")
    public ResponseEntity<ItemRepresentation> partialUpdateItem(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        log.info("Partially updating item {}", id);
        ItemRepresentation updatedItem = itemService.partialUpdateItem(id, updates);
        return ResponseEntity.ok(updatedItem);
    }

    // DELETE - Delete an item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        log.info("Deleting item {}", id);
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    // DELETE - Delete multiple items
    @DeleteMapping
    public ResponseEntity<Void> deleteItems(@RequestBody List<Long> ids) {
        log.info("Deleting {} items", ids.size());
        itemService.deleteItems(ids);
        return ResponseEntity.noContent().build();
    }

    // UTILITY - Check if item exists
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> itemExists(@PathVariable Long id) {
        boolean exists = itemService.itemExists(id);
        return ResponseEntity.ok(exists);
    }

    // UTILITY - Get total count
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCount() {
        long count = itemService.getTotalCount();
        return ResponseEntity.ok(count);
    }
}
