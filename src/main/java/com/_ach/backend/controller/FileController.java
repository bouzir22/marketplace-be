package com._ach.backend.controller;

import com._ach.backend.Model.ItemImageDTO;
import com._ach.backend.service.ImageService;
import com._ach.backend.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/files")
@CrossOrigin("*")
public class FileController {

    private final ImageService imageService;
    private final ItemService itemService;

    public FileController(ImageService imageService, ItemService itemService) {
        this.imageService = imageService;
        this.itemService = itemService;
    }

    // Upload a single image file (standalone, not attached to an item)
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadImage(@RequestPart("image") MultipartFile image) throws IOException {
        String imageUrl = imageService.uploadImage(image);
        return new ResponseEntity<>(imageUrl, HttpStatus.CREATED);
    }

    // Upload multiple image files (standalone, not attached to an item)
    @PostMapping(value = "/images/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ItemImageDTO>> uploadImages(
            @RequestParam("itemId") Long itemId,
            @RequestPart("images") List<MultipartFile> images) throws IOException {
        List<ItemImageDTO> uploadedImages = imageService.uploadImages(itemId, images);
        return new ResponseEntity<>(uploadedImages, HttpStatus.CREATED);
    }

    // ITEM IMAGE MANAGEMENT

    // Add image to item
    @PostMapping("/items/{id}/images")
    public ResponseEntity<ItemImageDTO> addImageToItem(
            @PathVariable Long id,
            @RequestBody ItemImageDTO imageDTO) {
        ItemImageDTO addedImage = itemService.addImage(id, imageDTO);
        return new ResponseEntity<>(addedImage, HttpStatus.CREATED);
    }

    // Upload image file to item
    @PostMapping(value = "/items/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemImageDTO> uploadImageToItem(
            @PathVariable Long id,
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "altText", required = false) String altText) throws IOException {

        String imageUrl = imageService.uploadImage(image);

        ItemImageDTO imageDTO = new ItemImageDTO();
        imageDTO.setUrl(imageUrl);
        imageDTO.setAltText(altText != null ? altText : image.getOriginalFilename());

        ItemImageDTO addedImage = itemService.addImage(id, imageDTO);
        return new ResponseEntity<>(addedImage, HttpStatus.CREATED);
    }

    // Get all images for an item
    @GetMapping("/items/{id}/images")
    public ResponseEntity<List<ItemImageDTO>> getItemImages(@PathVariable Long id) {
        List<ItemImageDTO> images = itemService.getItemImages(id);
        return ResponseEntity.ok(images);
    }

    // Set main image
    @PutMapping("/items/{itemId}/images/{imageId}/main")
    public ResponseEntity<Void> setMainImage(
            @PathVariable Long itemId,
            @PathVariable Long imageId) {
        itemService.setMainImage(itemId, imageId);
        return ResponseEntity.ok().build();
    }

    // Update image details
    @PutMapping("/items/{itemId}/images/{imageId}")
    public ResponseEntity<ItemImageDTO> updateImage(
            @PathVariable Long itemId,
            @PathVariable Long imageId,
            @RequestBody ItemImageDTO imageDTO) {
        ItemImageDTO updatedImage = itemService.updateImage(itemId, imageId, imageDTO);
        return ResponseEntity.ok(updatedImage);
    }

    // Delete image
    @DeleteMapping("/items/{itemId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long itemId,
            @PathVariable Long imageId) {
        itemService.deleteImage(itemId, imageId);
        return ResponseEntity.noContent().build();
    }

    // Reorder images
    @PutMapping("/items/{id}/images/reorder")
    public ResponseEntity<Void> reorderImages(
            @PathVariable Long id,
            @RequestBody List<Long> imageIds) {
        itemService.reorderImages(id, imageIds);
        return ResponseEntity.ok().build();
    }

    // Delete image file by URL
    @DeleteMapping("/images")
    public ResponseEntity<Void> deleteImageByUrl(@RequestParam("url") String imageUrl) throws IOException {
        imageService.deleteImage(imageUrl);
        return ResponseEntity.noContent().build();
    }
}
