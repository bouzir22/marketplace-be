package com._ach.backend.service;

import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.dao.ItemDao;
import com._ach.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final ItemDao itemDao;
    private final ImageStorageService imageStorageService;

    // CREATE - Create a new item
    public ItemRepresentation createItem(ItemRepresentation item) {
        log.info("Creating new item in service layer");
        return itemDao.create(item);
    }

    // CREATE - Create item with image upload
    public ItemRepresentation createItemWithImages(
            MultipartFile mainImage,
            List<MultipartFile> additionalImages,
            Map<String, Object> attributes) throws IOException {

        ItemRepresentation item = new ItemRepresentation();

        // Upload main image if provided
        if (mainImage != null && !mainImage.isEmpty()) {
            String imageUrl = imageStorageService.uploadImage(mainImage);
            item.setImage(imageUrl);
            log.info("Main image uploaded: {}", imageUrl);
        }

        // Upload additional images if provided
        if (additionalImages != null && !additionalImages.isEmpty()) {
            List<String> imageUrls = imageStorageService.uploadImages(additionalImages);
            item.setImages(imageUrls);
            log.info("Additional images uploaded: {}", imageUrls.size());
        }

        // Set attributes
        item.setAttributes(attributes);

        return itemDao.create(item);
    }

    // READ - Get all items with pagination and filters
    public List<ItemRepresentation> getAllItems(Pageable pageable, Map<String, String> filters) {
        log.info("Getting all items with pagination");
        return itemDao.findAll(pageable, filters);
    }

    // READ - Get item by ID
    public ItemRepresentation getItemById(Long id) {
        log.info("Getting item by id: {}", id);
        return itemDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
    }

    // UPDATE - Full update of an item
    public ItemRepresentation updateItem(Long id, ItemRepresentation itemDetails) {
        log.info("Updating item with id: {}", id);

        // Verify item exists
        ItemRepresentation existingItem = getItemById(id);

        // Set the ID for update
        itemDetails.setId(id);

        return itemDao.update(itemDetails);
    }

    // UPDATE - Update item with new images
    public ItemRepresentation updateItemWithImages(
            Long id,
            MultipartFile mainImage,
            List<MultipartFile> additionalImages,
            Map<String, Object> attributes) throws IOException {

        log.info("Updating item {} with new images", id);

        // Get existing item
        ItemRepresentation existingItem = getItemById(id);

        // Delete old main image if new one is provided
        if (mainImage != null && !mainImage.isEmpty()) {
            if (existingItem.getImage() != null) {
                imageStorageService.deleteImage(existingItem.getImage());
            }
            String imageUrl = imageStorageService.uploadImage(mainImage);
            existingItem.setImage(imageUrl);
            log.info("Main image updated: {}", imageUrl);
        }

        // Delete old additional images if new ones are provided
        if (additionalImages != null && !additionalImages.isEmpty()) {
            if (existingItem.getImages() != null && !existingItem.getImages().isEmpty()) {
                imageStorageService.deleteImages(existingItem.getImages());
            }
            List<String> imageUrls = imageStorageService.uploadImages(additionalImages);
            existingItem.setImages(imageUrls);
            log.info("Additional images updated: {}", imageUrls.size());
        }

        // Update attributes
        if (attributes != null) {
            existingItem.setAttributes(attributes);
        }

        return itemDao.update(existingItem);
    }

    // UPDATE - Partial update of an item
    public ItemRepresentation partialUpdateItem(Long id, Map<String, Object> updates) {
        log.info("Partially updating item with id: {}", id);

        // Get existing item
        ItemRepresentation existingItem = getItemById(id);

        // Update attributes by merging
        if (existingItem.getAttributes() != null) {
            existingItem.getAttributes().putAll(updates);
        } else {
            existingItem.setAttributes(updates);
        }

        return itemDao.update(existingItem);
    }

    // DELETE - Delete a single item
    public void deleteItem(Long id) {
        log.info("Deleting item with id: {}", id);
        // The DAO will handle deletion from all three storage locations
        itemDao.delete(id);
    }

    // DELETE - Delete multiple items
    public void deleteItems(List<Long> ids) {
        log.info("Deleting multiple items: {}", ids.size());
        for (Long id : ids) {
            itemDao.delete(id);
        }
    }

    // UTILITY - Check if item exists
    public boolean itemExists(Long id) {
        return itemDao.exists(id);
    }

    // UTILITY - Get total count
    public long getTotalCount() {
        return itemDao.count();
    }
}