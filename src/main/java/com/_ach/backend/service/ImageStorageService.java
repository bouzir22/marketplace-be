package com._ach.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Upload a single image to S3 and return the URL
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String fileName = generateFileName(file.getOriginalFilename());

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            String imageUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
            log.info("Image uploaded successfully: {}", imageUrl);

            return imageUrl;
        } catch (Exception e) {
            log.error("Error uploading image to S3", e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    /**
     * Upload multiple images to S3 and return the list of URLs
     */
    public List<String> uploadImages(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String url = uploadImage(file);
                if (url != null) {
                    imageUrls.add(url);
                }
            }
        }
        return imageUrls;
    }

    /**
     * Delete an image from S3 by URL
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            String fileName = extractFileNameFromUrl(imageUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Image deleted successfully: {}", imageUrl);
        } catch (Exception e) {
            log.error("Error deleting image from S3: {}", imageUrl, e);
        }
    }

    /**
     * Delete multiple images from S3
     */
    public void deleteImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        for (String url : imageUrls) {
            deleteImage(url);
        }
    }

    /**
     * Generate a unique file name for the image
     */
    private String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return "items/" + UUID.randomUUID().toString() + extension;
    }

    /**
     * Extract the file name from the S3 URL
     */
    private String extractFileNameFromUrl(String url) {
        if (url.contains("amazonaws.com/")) {
            return url.substring(url.indexOf("amazonaws.com/") + 14);
        }
        // For custom endpoint URLs
        if (url.contains(bucketName + "/")) {
            return url.substring(url.indexOf(bucketName + "/") + bucketName.length() + 1);
        }
        return url;
    }
}
