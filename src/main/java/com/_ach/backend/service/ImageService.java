package com._ach.backend.service;

import com._ach.backend.Model.ItemImageDTO;
import com._ach.backend.entity.Item;
import com._ach.backend.entity.ItemImage;
import com._ach.backend.repository.ItemImageRepository;
import com._ach.backend.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    @Value("${app.upload.base-url:/images}")
    private String baseUrl;

    public ImageService(ItemRepository itemRepository, ItemImageRepository itemImageRepository) {
        this.itemRepository = itemRepository;
        this.itemImageRepository = itemImageRepository;
    }

    /**
     * Upload a single image file and return the URL (without creating ItemImage entity)
     */
    public String uploadImage(MultipartFile file) throws IOException {
        validateFile(file);
        
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        Path uploadPath = getUploadPath();
        Path filePath = uploadPath.resolve(fileName);
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return baseUrl + "/" + fileName;
    }

    /**
     * Upload a single image file, create ItemImage, save it and return ItemImageDTO
     */
    public ItemImageDTO uploadImage(Long itemId, MultipartFile file, boolean isMain, Integer displayOrder, String altText) throws IOException {
        validateFile(file);
        
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + itemId));
        
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        Path uploadPath = getUploadPath();
        Path filePath = uploadPath.resolve(fileName);
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        String imageUrl = baseUrl + "/" + fileName;
        
        ItemImage itemImage = new ItemImage();
        itemImage.setItem(item);
        itemImage.setUrl(imageUrl);
        itemImage.setMain(isMain);
        itemImage.setDisplayOrder(displayOrder);
        itemImage.setAltText(altText);
        
        ItemImage savedImage = itemImageRepository.save(itemImage);
        
        return toDTO(savedImage);
    }
    
    private ItemImageDTO toDTO(ItemImage itemImage) {
        ItemImageDTO dto = new ItemImageDTO();
        dto.setId(itemImage.getId());
        dto.setUrl(itemImage.getUrl());
        dto.setMain(itemImage.isMain());
        dto.setDisplayOrder(itemImage.getDisplayOrder());
        dto.setAltText(itemImage.getAltText());
        return dto;
    }

    /**
     * Upload multiple image files and return their ItemImageDTOs
     */
    public List<ItemImageDTO> uploadImages(Long itemId, List<MultipartFile> files) throws IOException {
        List<ItemImageDTO> dtos = new ArrayList<>();
        int order = 0;
        for (MultipartFile file : files) {
            dtos.add(uploadImage(itemId, file, order == 0, order, null));
            order++;
        }
        return dtos;
    }

    /**
     * Delete an image by its URL
     */
    public void deleteImage(String imageUrl) throws IOException {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        Path filePath = Paths.get(uploadDir).resolve(fileName);
        
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    /**
     * Delete multiple images by their URLs
     */
    public void deleteImages(List<String> imageUrls) throws IOException {
        for (String url : imageUrls) {
            deleteImage(url);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Max file size: 10MB
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
    }

    private String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private Path getUploadPath() throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        return uploadPath;
    }
}
