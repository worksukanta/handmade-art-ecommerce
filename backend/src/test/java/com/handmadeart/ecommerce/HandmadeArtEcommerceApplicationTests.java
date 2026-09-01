package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.repository.AppUserRepository;
import com.handmadeart.ecommerce.repository.CategoryRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.ProductImageRepository;
import com.handmadeart.ecommerce.repository.ProductRelatedRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies that the Spring application context loads successfully
 * without requiring a live database connection.
 *
 * Repositories are mocked because the test application.yml excludes
 * DataSource and JPA auto-configuration (no PostgreSQL needed for this test).
 * All repositories transitively required by the full Spring context must be listed here.
 * AdminCatalogueService uses @Value for upload dir — no additional mock needed (uses default).
 */
@SpringBootTest
class HandmadeArtEcommerceApplicationTests {

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductImageRepository productImageRepository;

    @MockitoBean
    private ProductRelatedRepository productRelatedRepository;

    @MockitoBean
    private InventoryRepository inventoryRepository;

    @Test
    void contextLoads() {
        // If the Spring context starts without error, this test passes.
    }

}
