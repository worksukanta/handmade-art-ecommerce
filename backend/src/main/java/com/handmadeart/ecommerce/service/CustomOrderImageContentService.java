package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderImage;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomOrderImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Transactional(readOnly = true)
public class CustomOrderImageContentService {
    private final Path uploadRoot;
    private final CustomOrderImageRepository imageRepository;

    public CustomOrderImageContentService(
            @Value("${app.upload.reference-images:uploads/reference-images}") String uploadDir,
            CustomOrderImageRepository imageRepository) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.imageRepository = imageRepository;
    }

    public ImageContent getImage(AppUser currentUser, Long imageId) {
        CustomOrderImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Reference image not found"));
        boolean admin = currentUser.getRole() == UserRole.ADMIN;
        boolean owner = image.getCustomOrderRequest().getUser().getId().equals(currentUser.getId());
        if (!admin && !owner) throw new ResourceNotFoundException("Reference image not found");

        Path imagePath = resolveStoredImage(image.getStorageReference());
        MediaType contentType = parseImageContentType(image.getContentType());
        try {
            return new ImageContent(Files.readAllBytes(imagePath), contentType);
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Reference image content not found");
        }
    }

    private Path resolveStoredImage(String storageReference) {
        try {
            Path relative = Paths.get(storageReference);
            if (relative.isAbsolute()) throw new ResourceNotFoundException("Reference image content not found");
            Path candidate = uploadRoot.resolve(relative).normalize();
            if (!candidate.startsWith(uploadRoot) || !Files.isRegularFile(candidate))
                throw new ResourceNotFoundException("Reference image content not found");
            Path realRoot = uploadRoot.toRealPath();
            Path realFile = candidate.toRealPath();
            if (!realFile.startsWith(realRoot) || !Files.isRegularFile(realFile))
                throw new ResourceNotFoundException("Reference image content not found");
            return realFile;
        } catch (InvalidPathException | IOException | NullPointerException ex) {
            throw new ResourceNotFoundException("Reference image content not found");
        }
    }

    private MediaType parseImageContentType(String contentType) {
        try {
            MediaType type = MediaType.parseMediaType(contentType);
            if (!"image".equalsIgnoreCase(type.getType())) throw new ResourceNotFoundException("Reference image content not found");
            return type;
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResourceNotFoundException("Reference image content not found");
        }
    }

    public record ImageContent(byte[] bytes, MediaType contentType) {}
}
