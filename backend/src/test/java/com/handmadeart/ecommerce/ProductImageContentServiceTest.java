package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.catalogue.ProductImageResponse;
import com.handmadeart.ecommerce.entity.ProductImage;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.ProductImageRepository;
import com.handmadeart.ecommerce.service.ProductImageContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageContentServiceTest {

    @TempDir
    Path tempDirectory;

    @Mock
    ProductImageRepository productImageRepository;

    private Path uploadRoot;
    private ProductImageContentService service;

    @BeforeEach
    void setUp() throws Exception {
        uploadRoot = Files.createDirectory(tempDirectory.resolve("product-images"));
        service = new ProductImageContentService(uploadRoot.toString(), productImageRepository);
    }

    @Test
    @DisplayName("IMG-DEL-01: active product image returns stored bytes and content type")
    void validImage_returnsBytesAndContentType() throws Exception {
        byte[] expected = new byte[] {1, 2, 3, 4};
        Path productDirectory = Files.createDirectory(uploadRoot.resolve("product-10"));
        Files.write(productDirectory.resolve("image.png"), expected);
        ProductImage image = image("product-10/image.png", "image/png");
        when(productImageRepository.findPublicImage(7L, ProductStatus.ACTIVE))
                .thenReturn(Optional.of(image));

        ProductImageContentService.ProductImageContent result = service.getPublicImage(7L);

        assertThat(result.bytes()).isEqualTo(expected);
        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    @DisplayName("IMG-DEL-02: unknown or non-public image id returns not found")
    void unknownImage_returnsNotFound() {
        when(productImageRepository.findPublicImage(404L, ProductStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicImage(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product image not found");
    }

    @Test
    @DisplayName("IMG-DEL-03: traversal storage reference cannot escape upload root")
    void traversalReference_isRejected() throws Exception {
        Files.write(tempDirectory.resolve("outside.png"), new byte[] {9});
        ProductImage image = image("../outside.png", "image/png");
        when(productImageRepository.findPublicImage(8L, ProductStatus.ACTIVE))
                .thenReturn(Optional.of(image));

        assertThatThrownBy(() -> service.getPublicImage(8L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product image content not found");
    }

    @Test
    @DisplayName("IMG-DEL-04: product image response exposes browser content URL")
    void imageResponse_exposesContentUrl() {
        ProductImage image = image("product-10/image.png", "image/png");
        ReflectionTestUtils.setField(image, "id", 23L);

        ProductImageResponse response = ProductImageResponse.from(image);

        assertThat(response.getImageUrl()).isEqualTo("/api/v1/product-images/23/content");
        assertThat(response.getStorageReference()).isEqualTo("product-10/image.png");
    }

    private ProductImage image(String storageReference, String contentType) {
        ProductImage image = new ProductImage();
        image.setStorageReference(storageReference);
        image.setContentType(contentType);
        image.setFileSizeBytes(1);
        image.setDisplayOrder(0);
        return image;
    }
}
