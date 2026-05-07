package com._ach.backend.controller;

import com._ach.backend.Model.ItemImageDTO;
import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.entity.Item;
import com._ach.backend.service.ImageService;
import com._ach.backend.service.ItemService;
import com.querydsl.core.types.Predicate;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/items")
@CrossOrigin("*")
public class ItemController {
    private final ItemService itemService;
    private final ImageService imageService;

    public ItemController(ItemService itemService, ImageService imageService) {
        this.itemService = itemService;
        this.imageService = imageService;
    }

    // Note: File/image management endpoints have been moved to FileController

    // CREATE - Create a new item (JSON body)
    @PostMapping
    public ResponseEntity<ItemRepresentation> createItem(@RequestBody ItemRepresentation item) {
        ItemRepresentation createdItem = itemService.createItem(item);
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    // CREATE - Create a new item with image uploads (multipart/form-data)
    @PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemRepresentation> createItemWithImages(
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestParam("item") String itemJson) throws IOException {
        
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        ItemRepresentation item = objectMapper.readValue(itemJson, ItemRepresentation.class);
        
        // Upload images and create image DTOs
        if (images != null && !images.isEmpty()) {
            List<ItemImageDTO> imageDTOs = new ArrayList<>();
            for (int i = 0; i < images.size(); i++) {
                MultipartFile file = images.get(i);
                String imageUrl = imageService.uploadImage(file);
                
                ItemImageDTO imageDTO = new ItemImageDTO();
                imageDTO.setUrl(imageUrl);
                imageDTO.setMain(i == 0); // First image is main
                imageDTO.setDisplayOrder(i);
                imageDTO.setAltText(file.getOriginalFilename());
                imageDTOs.add(imageDTO);
            }
            item.setImages(imageDTOs);
        }
        
        ItemRepresentation createdItem = itemService.createItem(item);
        return new ResponseEntity<>(createdItem, HttpStatus.CREATED);
    }

    // READ - Get all items with optional filtering
    @GetMapping
    public ResponseEntity<List<ItemRepresentation>> filterItems(
            @QuerydslPredicate(root = Item.class) Predicate predicate) {
        List<ItemRepresentation> items = itemService.filterItems(predicate);
        return ResponseEntity.ok(items);
    }

    // READ - Get item by ID
    @GetMapping("/{id}")
    public ResponseEntity<ItemRepresentation> getItemById(@PathVariable Long id) {
        ItemRepresentation item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    // UPDATE - Update an existing item
    @PutMapping("/{id}")
    public ResponseEntity<ItemRepresentation> updateItem(
            @PathVariable Long id,
             @RequestBody ItemRepresentation item) {
        ItemRepresentation updatedItem = itemService.updateItem(id, item);
        return ResponseEntity.ok(updatedItem);
    }

    // UPDATE - Partial update (PATCH)
    @PatchMapping("/{id}")
    public ResponseEntity<ItemRepresentation> partialUpdateItem(
            @PathVariable Long id,
            @RequestBody ItemRepresentation item) {
        ItemRepresentation updatedItem = itemService.partialUpdateItem(id, item);
        return ResponseEntity.ok(updatedItem);
    }

    // DELETE - Delete an item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    // DELETE - Delete multiple items
    @DeleteMapping
    public ResponseEntity<Void> deleteItems(@RequestBody List<Long> ids) {
        itemService.deleteItems(ids);
        return ResponseEntity.noContent().build();
    }
}
