package com._ach.backend.service;

import com._ach.backend.Model.ItemImageDTO;
import com._ach.backend.Model.ItemRepresentation;
import com._ach.backend.document.ItemDetails;
import com._ach.backend.entity.Item;
import com._ach.backend.entity.ItemImage;
import com._ach.backend.repository.ItemImageRepository;
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
    private final ItemImageRepository itemImageRepository;
    private final ItemDetailsService itemDetailsService;

    public ItemService(ItemRepository itemRepository, ItemImageRepository itemImageRepository, ItemDetailsService itemDetailsService) {
        this.itemRepository = itemRepository;
        this.itemImageRepository = itemImageRepository;
        this.itemDetailsService = itemDetailsService;
    }

    // CREATE - Create a new item
    public ItemRepresentation createItem(ItemRepresentation itemRepresentation) {
        // Step 1: Save item details to Elasticsearch first
        String itemDetailsId = itemDetailsService.saveItemDetails(itemRepresentation.getAttributes());

        // Step 2: Create Item entity with the Elasticsearch ID
        Item item = new Item();
        item.setAttributesMapId(itemDetailsId);

        // Step 3: Save to PostgreSQL
        Item savedItem = itemRepository.save(item);

        // Step 4: Add images if provided
        if (itemRepresentation.getImages() != null && !itemRepresentation.getImages().isEmpty()) {
            for (int i = 0; i < itemRepresentation.getImages().size(); i++) {
                ItemImageDTO imageDTO = itemRepresentation.getImages().get(i);
                ItemImage image = toItemImage(imageDTO);
                if (image.getDisplayOrder() == null) {
                    image.setDisplayOrder(i);
                }
                savedItem.addImage(image);
            }
            savedItem = itemRepository.save(savedItem);
        }

        // Step 5: Return representation
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

        // Update item details in Elasticsearch
        String itemDetailsId = itemDetailsService.updateItemDetails(
                item.getAttributesMapId(),
                itemRepresentation.getAttributes()
        );

        // Update item fields
        item.setAttributesMapId(itemDetailsId);

        // Update images if provided (replace all)
        if (itemRepresentation.getImages() != null) {
            item.getImages().clear();
            for (int i = 0; i < itemRepresentation.getImages().size(); i++) {
                ItemImageDTO imageDTO = itemRepresentation.getImages().get(i);
                ItemImage image = toItemImage(imageDTO);
                if (image.getDisplayOrder() == null) {
                    image.setDisplayOrder(i);
                }
                item.addImage(image);
            }
        }

        Item savedItem = itemRepository.save(item);
        return toRepresentation(savedItem);
    }

    // UPDATE - Partial update of an item
    public ItemRepresentation partialUpdateItem(Long id, ItemRepresentation itemRepresentation) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        // Update item details in Elasticsearch if provided
        if (itemRepresentation.getAttributes() != null) {
            String itemDetailsId = itemDetailsService.updateItemDetails(
                    item.getAttributesMapId(),
                    itemRepresentation.getAttributes()
            );
            item.setAttributesMapId(itemDetailsId);
        }

        // Update images if provided
        if (itemRepresentation.getImages() != null) {
            item.getImages().clear();
            for (int i = 0; i < itemRepresentation.getImages().size(); i++) {
                ItemImageDTO imageDTO = itemRepresentation.getImages().get(i);
                ItemImage image = toItemImage(imageDTO);
                if (image.getDisplayOrder() == null) {
                    image.setDisplayOrder(i);
                }
                item.addImage(image);
            }
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
            itemDetailsService.deleteItemDetails(item.getAttributesMapId());
        }

        // Delete from PostgreSQL
        itemRepository.delete(item);
    }

    // DELETE - Delete multiple items
    public void deleteItems(List<Long> ids) {
        List<Item> items = itemRepository.findAllById(ids);

        // Delete item details from Elasticsearch
        items.forEach(item -> {
            if (item.getAttributesMapId() != null) {
                itemDetailsService.deleteItemDetails(item.getAttributesMapId());
            }
        });

        // Delete from PostgreSQL
        itemRepository.deleteAllById(ids);
    }

    // Helper method to convert Item to ItemRepresentation
    private ItemRepresentation toRepresentation(Item item) {
        ItemRepresentation representation = new ItemRepresentation();
        representation.setId(item.getId());
        
        // Convert images to DTOs
        List<ItemImageDTO> imageDTOs = item.getImages().stream()
                .map(this::toImageDTO)
                .toList();
        representation.setImages(imageDTOs);

        // Fetch item details from Elasticsearch
        if (item.getAttributesMapId() != null) {
            ItemDetails itemDetails = itemDetailsService.getItemDetails(item.getAttributesMapId());
            if (itemDetails != null) {
                representation.setAttributes(itemDetails.getAttributes());
            }
        }

        return representation;
    }

    // IMAGE MANAGEMENT METHODS

    // Add image to item
    public ItemImageDTO addImage(Long itemId, ItemImageDTO imageDTO) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        ItemImage image = toItemImage(imageDTO);
        
        // Set display order if not provided
        if (image.getDisplayOrder() == null) {
            image.setDisplayOrder((int) itemImageRepository.countByItemId(itemId));
        }
        
        // If this is the first image or marked as main, set it as main
        if (item.getImages().isEmpty() || imageDTO.isMain()) {
            item.setMainImage(image);
        }
        
        item.addImage(image);
        itemRepository.save(item);
        
        return toImageDTO(image);
    }

    // Get all images for an item
    public List<ItemImageDTO> getItemImages(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found with id: " + itemId);
        }
        return itemImageRepository.findByItemIdOrderByDisplayOrderAsc(itemId).stream()
                .map(this::toImageDTO)
                .toList();
    }

    // Set main image
    public void setMainImage(Long itemId, Long imageId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        ItemImage image = itemImageRepository.findByItemIdAndImageId(itemId, imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId + " for item: " + itemId));

        item.setMainImage(image);
        itemRepository.save(item);
    }

    // Update image details
    public ItemImageDTO updateImage(Long itemId, Long imageId, ItemImageDTO imageDTO) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found with id: " + itemId);
        }

        ItemImage image = itemImageRepository.findByItemIdAndImageId(itemId, imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId + " for item: " + itemId));

        if (imageDTO.getUrl() != null) {
            image.setUrl(imageDTO.getUrl());
        }
        if (imageDTO.getAltText() != null) {
            image.setAltText(imageDTO.getAltText());
        }
        if (imageDTO.getDisplayOrder() != null) {
            image.setDisplayOrder(imageDTO.getDisplayOrder());
        }
        if (imageDTO.isMain()) {
            Item item = image.getItem();
            item.setMainImage(image);
        }

        itemImageRepository.save(image);
        return toImageDTO(image);
    }

    // Delete image
    public void deleteImage(Long itemId, Long imageId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        ItemImage image = itemImageRepository.findByItemIdAndImageId(itemId, imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId + " for item: " + itemId));

        boolean wasMain = image.isMain();
        item.removeImage(image);
        itemImageRepository.delete(image);

        // If deleted image was main, set new main image
        if (wasMain && !item.getImages().isEmpty()) {
            item.setMainImage(item.getImages().get(0));
            itemRepository.save(item);
        }
    }

    // Reorder images
    public void reorderImages(Long itemId, List<Long> imageIds) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found with id: " + itemId);
        }

        List<ItemImage> images = itemImageRepository.findByItemIdOrderByDisplayOrderAsc(itemId);
        
        for (int i = 0; i < imageIds.size(); i++) {
            Long imageId = imageIds.get(i);
            for (ItemImage image : images) {
                if (image.getId().equals(imageId)) {
                    image.setDisplayOrder(i);
                    itemImageRepository.save(image);
                    break;
                }
            }
        }
    }

    // Helper method to convert ItemImage to DTO
    private ItemImageDTO toImageDTO(ItemImage image) {
        ItemImageDTO dto = new ItemImageDTO();
        dto.setId(image.getId());
        dto.setUrl(image.getUrl());
        dto.setMain(image.isMain());
        dto.setDisplayOrder(image.getDisplayOrder());
        dto.setAltText(image.getAltText());
        return dto;
    }

    // Helper method to convert DTO to ItemImage
    private ItemImage toItemImage(ItemImageDTO dto) {
        ItemImage image = new ItemImage();
        image.setUrl(dto.getUrl());
        image.setMain(dto.isMain());
        image.setDisplayOrder(dto.getDisplayOrder());
        image.setAltText(dto.getAltText());
        return image;
    }
}