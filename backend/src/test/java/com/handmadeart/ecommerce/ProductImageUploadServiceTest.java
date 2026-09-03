package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductImage;
import com.handmadeart.ecommerce.repository.CategoryRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.ProductImageRepository;
import com.handmadeart.ecommerce.repository.ProductRelatedRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageUploadServiceTest {
    @TempDir Path temporaryDirectory;
    @Mock CategoryRepository categoryRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductImageRepository imageRepository;
    @Mock ProductRelatedRepository relatedRepository;
    @Mock InventoryRepository inventoryRepository;

    @Test void relativeConfiguredRootStoresFileAtNormalizedAbsoluteDestination() throws Exception {
        Path relativeRoot = Path.of("target", "upload-test-" + System.nanoTime());
        Product product = new Product(); ReflectionTestUtils.setField(product, "id", 12L);
        when(productRepository.findById(12L)).thenReturn(Optional.of(product));
        when(imageRepository.countByProductId(12L)).thenReturn(0L);
        when(imageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> {
            ProductImage image = invocation.getArgument(0); ReflectionTestUtils.setField(image, "id", 9L); return image;
        });
        AdminCatalogueService service = new AdminCatalogueService(relativeRoot.toString(), categoryRepository, productRepository, imageRepository, relatedRepository, inventoryRepository);
        try {
            var response = service.addProductImage(12L, new MockMultipartFile("file", "valid.png", "image/png", new byte[]{1, 2, 3}));
            Path stored = relativeRoot.toAbsolutePath().normalize().resolve(response.getStorageReference());
            assertThat(Files.readAllBytes(stored)).containsExactly(1, 2, 3);
        } finally {
            Path absolute = relativeRoot.toAbsolutePath().normalize();
            if (Files.exists(absolute)) try (var paths = Files.walk(absolute)) { paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> { try { Files.delete(path); } catch (Exception ignored) {} }); }
        }
    }
}
