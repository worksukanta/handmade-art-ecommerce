package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.config.DevelopmentDataSeeder;
import com.handmadeart.ecommerce.entity.*;
import com.handmadeart.ecommerce.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevelopmentDataSeederTest {

    @TempDir Path tempDirectory;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AppUserRepository userRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ProductRepository productRepository;
    @Mock InventoryRepository inventoryRepository;
    @Mock ProductRelatedRepository relatedRepository;
    @Mock ProductImageRepository imageRepository;
    @Mock ApplicationArguments arguments;

    @Test
    void disabledSeeder_createsNothing() throws Exception {
        seeder(false, "", "").run(arguments);

        verifyNoInteractions(passwordEncoder, userRepository, categoryRepository, productRepository,
                inventoryRepository, relatedRepository, imageRepository);
    }

    @Test
    void enabledSeeder_createsEncodedAdminAndSampleCatalogue() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("developer-secret")).thenReturn("encoded-secret");
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(productRepository.findAll()).thenReturn(List.of());
        when(inventoryRepository.findByProductId(any())).thenReturn(Optional.empty());
        when(relatedRepository.existsById(any())).thenReturn(false);
        when(imageRepository.findByProductIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());

        AtomicLong categoryIds = new AtomicLong(1);
        when(categoryRepository.save(any())).thenAnswer(call -> {
            Category saved = call.getArgument(0);
            return withId(saved, categoryIds.getAndIncrement());
        });
        AtomicLong productIds = new AtomicLong(10);
        when(productRepository.save(any())).thenAnswer(call -> {
            Product saved = call.getArgument(0);
            return withId(saved, productIds.getAndIncrement());
        });

        seeder(true, "Admin@Example.com", "developer-secret").run(arguments);

        ArgumentCaptor<AppUser> adminCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(adminCaptor.capture());
        assertThat(adminCaptor.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(adminCaptor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(adminCaptor.getValue().getPasswordHash()).isEqualTo("encoded-secret");
        verify(passwordEncoder).encode("developer-secret");

        verify(categoryRepository, times(3)).save(any(Category.class));
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(4)).save(productCaptor.capture());
        assertThat(productCaptor.getAllValues()).extracting(Product::getProductType)
                .containsExactly(ProductType.READY_MADE, ProductType.READY_MADE,
                        ProductType.CUSTOM_AVAILABLE, ProductType.PORTFOLIO_ONLY);

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(3)).save(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getAllValues()).extracting(Inventory::getQuantityOnHand)
                .containsExactly(5, 1, 0);
        verify(relatedRepository).save(any(ProductRelated.class));
        verify(imageRepository, times(3)).save(any(ProductImage.class));
        assertThat(Files.walk(tempDirectory).filter(Files::isRegularFile).count()).isEqualTo(3);
    }

    @Test
    void rerunWithExistingSeedData_createsNoDuplicates() throws Exception {
        AppUser admin = new AppUser();
        admin.setRole(UserRole.ADMIN);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        Category clothing = category(1L, "Painted Clothing");
        Category portraits = category(2L, "Portraits");
        Category accessories = category(3L, "Handmade Accessories");
        when(categoryRepository.findByName("Painted Clothing")).thenReturn(Optional.of(clothing));
        when(categoryRepository.findByName("Portraits")).thenReturn(Optional.of(portraits));
        when(categoryRepository.findByName("Handmade Accessories")).thenReturn(Optional.of(accessories));

        Product a = product(10L, "Hand-Painted Botanical Denim Jacket", clothing, ProductType.READY_MADE);
        Product b = product(11L, "Hand-Painted Midnight Floral Shirt", clothing, ProductType.READY_MADE);
        Product c = product(12L, "Custom Watercolour Family Portrait", portraits, ProductType.CUSTOM_AVAILABLE);
        Product d = product(13L, "Golden Hour Heritage Portrait", portraits, ProductType.PORTFOLIO_ONLY);
        when(productRepository.findAll()).thenReturn(List.of(a, b, c, d));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory(a, 5)));
        when(inventoryRepository.findByProductId(11L)).thenReturn(Optional.of(inventory(b, 1)));
        when(inventoryRepository.findByProductId(12L)).thenReturn(Optional.of(inventory(c, 0)));
        when(relatedRepository.existsById(new ProductRelatedId(10L, 11L))).thenReturn(true);
        when(imageRepository.findByProductIdOrderByDisplayOrderAsc(anyLong()))
                .thenReturn(List.of(image("seed-botanical-jacket.png")),
                        List.of(image("seed-floral-shirt.png")),
                        List.of(image("seed-portrait-sample.png")));

        seeder(true, "admin@example.com", "developer-secret").run(arguments);

        verify(userRepository, never()).save(any());
        verify(categoryRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
        verify(relatedRepository, never()).save(any());
        verify(imageRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    private DevelopmentDataSeeder seeder(boolean enabled, String email, String password) {
        return new DevelopmentDataSeeder(enabled, email, password, tempDirectory.toString(),
                passwordEncoder, userRepository, categoryRepository, productRepository,
                inventoryRepository, relatedRepository, imageRepository);
    }

    private <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private Category category(long id, String name) {
        Category value = new Category(); value.setName(name); return withId(value, id);
    }

    private Product product(long id, String name, Category category, ProductType type) {
        Product value = new Product(); value.setName(name); value.setCategory(category); value.setProductType(type); return withId(value, id);
    }

    private Inventory inventory(Product product, int quantity) {
        Inventory value = new Inventory(); value.setProduct(product); value.setQuantityOnHand(quantity); return value;
    }

    private ProductImage image(String filename) {
        ProductImage value = new ProductImage(); value.setOriginalFilename(filename); return value;
    }
}
