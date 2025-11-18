package com._ach.backend.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling image uploads to remote storage (AWS S3 or compatible service).
 * Supports multiple image uploads and returns publicly accessible URLs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucket:marketplace-images}")
    private String bucketName;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    /**
     * Upload a single image to S3 and return the public URL
     *
     * @param file The image file to upload
     * @return The public URL of the uploaded image
     * @throws IOException if upload fails
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (!s3Enabled) {
            log.warn("S3 is disabled. Returning mock URL for file: {}", file.getOriginalFilename());
            return generateMockUrl(file.getOriginalFilename());
        }

        String fileName = generateFileName(file.getOriginalFilename());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName,
                fileName,
                file.getInputStream(),
                metadata
        ).withCannedAcl(CannedAccessControlList.PublicRead);

        amazonS3.putObject(putObjectRequest);

        String imageUrl = amazonS3.getUrl(bucketName, fileName).toString();
        log.info("Image uploaded successfully: {}", imageUrl);

        return imageUrl;
    }

    /**
     * Upload multiple images to S3 and return their public URLs
     *
     * @param files List of image files to upload
     * @return List of public URLs of the uploaded images
     * @throws IOException if any upload fails
     */
    public List<String> uploadImages(List<MultipartFile> files) throws IOException {
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String imageUrl = uploadImage(file);
                imageUrls.add(imageUrl);
            }
        }

        return imageUrls;
    }

    /**
     * Delete an image from S3
     *
     * @param imageUrl The URL of the image to delete
     */
    public void deleteImage(String imageUrl) {
        if (!s3Enabled) {
            log.warn("S3 is disabled. Cannot delete image: {}", imageUrl);
            return;
        }

        try {
            String fileName = extractFileNameFromUrl(imageUrl);
            amazonS3.deleteObject(bucketName, fileName);
            log.info("Image deleted successfully: {}", imageUrl);
        } catch (Exception e) {
            log.error("Error deleting image: {}", imageUrl, e);
        }
    }

    /**
     * Delete multiple images from S3
     *
     * @param imageUrls List of image URLs to delete
     */
    public void deleteImages(List<String> imageUrls) {
        if (imageUrls != null) {
            imageUrls.forEach(this::deleteImage);
        }
    }

    /**
     * Generate a unique file name for the uploaded image
     *
     * @param originalFileName Original file name
     * @return Unique file name with UUID prefix
     */
    private String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Extract file name from S3 URL
     *
     * @param imageUrl The full S3 URL
     * @return The file name
     */
    private String extractFileNameFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }

    /**
     * Generate a mock URL for testing when S3 is disabled
     *
     * @param fileName Original file name
     * @return Mock URL
     */
    private String generateMockUrl(String fileName) {
        return "https://mock-storage.example.com/images/" + generateFileName(fileName);
    }
}
