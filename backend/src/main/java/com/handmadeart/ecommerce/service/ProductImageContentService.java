package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.entity.ProductImage;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.ProductImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves and reads public catalogue product images from controlled filesystem storage. */
@Service
@Transactional(readOnly = true)
public class ProductImageContentService {

    private final Path uploadRoot;
    private final ProductImageRepository productImageRepository;

    public ProductImageContentService(
            @Value("${app.upload.product-images:uploads/product-images}") String uploadDir,
            ProductImageRepository productImageRepository) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.productImageRepository = productImageRepository;
    }

    public ProductImageContent getPublicImage(Long imageId) {
        ProductImage image = productImageRepository
                .findPublicImage(imageId, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found"));

        Path imagePath = resolveStoredImage(image.getStorageReference());
        MediaType contentType = parseImageContentType(image.getContentType());

        try {
            return new ProductImageContent(Files.readAllBytes(imagePath), contentType);
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Product image content not found");
        }
    }

    private Path resolveStoredImage(String storageReference) {
        final Path relativePath;
        try {
            relativePath = Paths.get(storageReference);
        } catch (InvalidPathException | NullPointerException ex) {
            throw new ResourceNotFoundException("Product image content not found");
        }

        if (relativePath.isAbsolute()) {
            throw new ResourceNotFoundException("Product image content not found");
        }

        Path candidate = uploadRoot.resolve(relativePath).normalize();
        if (!candidate.startsWith(uploadRoot) || !Files.isRegularFile(candidate)) {
            throw new ResourceNotFoundException("Product image content not found");
        }

        try {
            Path realRoot = uploadRoot.toRealPath();
            Path realFile = candidate.toRealPath();
            if (!realFile.startsWith(realRoot) || !Files.isRegularFile(realFile)) {
                throw new ResourceNotFoundException("Product image content not found");
            }
            return realFile;
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Product image content not found");
        }
    }

    private MediaType parseImageContentType(String contentType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            if (!"image".equalsIgnoreCase(mediaType.getType())) {
                throw new ResourceNotFoundException("Product image content not found");
            }
            return mediaType;
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResourceNotFoundException("Product image content not found");
        }
    }

    public record ProductImageContent(byte[] bytes, MediaType contentType) {
    }
}
