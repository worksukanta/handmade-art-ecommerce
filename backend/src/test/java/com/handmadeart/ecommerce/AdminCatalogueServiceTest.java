package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.admin.CategoryRequest;
import com.handmadeart.ecommerce.dto.admin.InventoryUpdateRequest;
import com.handmadeart.ecommerce.dto.admin.ProductRequest;
import com.handmadeart.ecommerce.dto.admin.RelatedProductsRequest;
import com.handmadeart.ecommerce.dto.catalogue.CategoryResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductDetailResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductSummaryResponse;
import com.handmadeart.ecommerce.entity.Category;
import com.handmadeart.ecommerce.entity.CategoryStatus;
import com.handmadeart.ecommerce.entity.Inventory;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductRelated;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import com.handmadeart.ecommerce.exception.DuplicateCategoryNameException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CategoryRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.ProductImageRepository;
import com.handmadeart.ecommerce.entity.ProductImage;
import com.handmadeart.ecommerce.repository.ProductRelatedRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the real {@link AdminCatalogueService} production logic.
 *
 * Uses Mockito only — no Spring context, no database, no filesystem access
 * (image upload tests require the real service; not covered here; covered in
 * integration tests or separate file-I/O tests outside this suite).
 *
 * Covered:
 *   CAT-01  create category succeeds — name, description, ACTIVE status
 *   CAT-02  create category rejects duplicate name → DuplicateCategoryNameException
 *   CAT-03  update category — name, description changed
 *   CAT-04  update category same name (case-insensitive) — no false duplicate conflict
 *   CAT-05  update category new name already taken → DuplicateCategoryNameException
 *   CAT-06  category not found → ResourceNotFoundException
 *   CAT-07  change category status INACTIVE succeeds
 *   CAT-08  change category status invalid value → IllegalArgumentException
 *
 *   PROD-01  create product with READY_MADE type creates inventory row
 *   PROD-02  create product with PORTFOLIO_ONLY type does NOT create inventory row
 *   PROD-03  create product with invalid category → ResourceNotFoundException
 *   PROD-04  create product with invalid productType → IllegalArgumentException
 *   PROD-05  update product changes core fields
 *   PROD-06  change product status INACTIVE succeeds
 *   PROD-07  product not found → ResourceNotFoundException
 *
 *   REL-01  replaceRelatedProducts clears existing and saves new set
 *   REL-02  self-reference rejected → IllegalArgumentException
 *   REL-03  invalid related product ID → ResourceNotFoundException
 *
 *   INV-01  updateInventory sets quantity on existing row
 *   INV-02  PORTFOLIO_ONLY product → InventoryTypeConflictException
 *   INV-03  product not found → ResourceNotFoundException
 *   INV-04  negative quantity rejected at DTO level (tested separately via validation)
 */
@ExtendWith(MockitoExtension.class)
class AdminCatalogueServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductRelatedRepository productRelatedRepository;
    @Mock private InventoryRepository inventoryRepository;

    private AdminCatalogueService service;

    @BeforeEach
    void setUp() {
        service = new AdminCatalogueService(
                "uploads/test",   // upload dir — never touched by these unit tests
                categoryRepository,
                productRepository,
                productImageRepository,
                productRelatedRepository,
                inventoryRepository
        );
    }

    // =========================================================================
    // Category tests
    // =========================================================================

    @Test
    @DisplayName("CAT-01: createCategory persists with ACTIVE status and returns CategoryResponse")
    void createCategory_success() {
        when(categoryRepository.existsByName("Paintings")).thenReturn(false);
        Category saved = buildCategory(1L, "Paintings", "Art works", CategoryStatus.ACTIVE);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryRequest req = new CategoryRequest();
        req.setName("Paintings");
        req.setDescription("Art works");

        CategoryResponse response = service.createCategory(req);

        assertThat(response.getName()).isEqualTo("Paintings");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CategoryStatus.ACTIVE);
    }

    @Test
    @DisplayName("CAT-02: createCategory rejects duplicate name with DuplicateCategoryNameException")
    void createCategory_duplicateName_throws() {
        when(categoryRepository.existsByName("Paintings")).thenReturn(true);

        CategoryRequest req = new CategoryRequest();
        req.setName("Paintings");

        assertThatThrownBy(() -> service.createCategory(req))
                .isInstanceOf(DuplicateCategoryNameException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("CAT-03: updateCategory changes name and description")
    void updateCategory_success() {
        Category existing = buildCategory(1L, "OldName", "Old desc", CategoryStatus.ACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("NewName")).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryRequest req = new CategoryRequest();
        req.setName("NewName");
        req.setDescription("New desc");

        CategoryResponse response = service.updateCategory(1L, req);

        assertThat(response.getName()).isEqualTo("NewName");
        assertThat(response.getDescription()).isEqualTo("New desc");
    }

    @Test
    @DisplayName("CAT-04: updateCategory with same name (case-insensitive) does not reject as duplicate")
    void updateCategory_sameNameCaseInsensitive_allowed() {
        Category existing = buildCategory(1L, "paintings", "desc", CategoryStatus.ACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryRequest req = new CategoryRequest();
        req.setName("paintings");  // same name — no false duplicate conflict
        req.setDescription("new desc");

        // Should not throw — same category name is allowed on update
        CategoryResponse response = service.updateCategory(1L, req);
        assertThat(response.getName()).isEqualTo("paintings");
        verify(categoryRepository, never()).existsByName("paintings"); // equalsIgnoreCase guard skips check
    }

    @Test
    @DisplayName("CAT-05: updateCategory with name taken by another category → DuplicateCategoryNameException")
    void updateCategory_nameConflict_throws() {
        Category existing = buildCategory(1L, "Paintings", "desc", CategoryStatus.ACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Sculptures")).thenReturn(true);

        CategoryRequest req = new CategoryRequest();
        req.setName("Sculptures");

        assertThatThrownBy(() -> service.updateCategory(1L, req))
                .isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    @DisplayName("CAT-06: updateCategory with unknown ID → ResourceNotFoundException")
    void updateCategory_notFound_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(99L, new CategoryRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("CAT-07: changeCategoryStatus sets INACTIVE status")
    void changeCategoryStatus_inactive_succeeds() {
        Category existing = buildCategory(1L, "Paintings", "desc", CategoryStatus.ACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = service.changeCategoryStatus(1L, "INACTIVE");

        assertThat(response.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("CAT-08: changeCategoryStatus with invalid value → IllegalArgumentException")
    void changeCategoryStatus_invalidValue_throws() {
        Category existing = buildCategory(1L, "Paintings", "desc", CategoryStatus.ACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.changeCategoryStatus(1L, "DELETED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DELETED");
    }

    // =========================================================================
    // Product tests
    // =========================================================================

    @Test
    @DisplayName("PROD-01: createProduct with READY_MADE type creates inventory row")
    void createProduct_readyMade_createsInventory() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Product savedProduct = buildProduct(10L, category, "Painting", ProductType.READY_MADE,
                ProductStatus.ACTIVE, new BigDecimal("50.00"));
        when(productRepository.save(any())).thenReturn(savedProduct);
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of());
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.empty());
        when(productRelatedRepository.findByProductId(10L)).thenReturn(List.of());

        ProductRequest req = buildProductRequest("Painting", "1", ProductType.READY_MADE.name(),
                ProductStatus.ACTIVE.name(), new BigDecimal("50.00"));

        service.createProduct(req);

        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("PROD-02: createProduct with PORTFOLIO_ONLY type does NOT create inventory row")
    void createProduct_portfolioOnly_noInventory() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Product savedProduct = buildProduct(10L, category, "Portfolio Piece", ProductType.PORTFOLIO_ONLY,
                ProductStatus.ACTIVE, new BigDecimal("0.00"));
        when(productRepository.save(any())).thenReturn(savedProduct);
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of());
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.empty());
        when(productRelatedRepository.findByProductId(10L)).thenReturn(List.of());

        ProductRequest req = buildProductRequest("Portfolio Piece", "1",
                ProductType.PORTFOLIO_ONLY.name(), ProductStatus.ACTIVE.name(), BigDecimal.ZERO);

        service.createProduct(req);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("PROD-03: createProduct with invalid categoryId → ResourceNotFoundException")
    void createProduct_invalidCategory_throws() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        ProductRequest req = buildProductRequest("Test", "999", ProductType.READY_MADE.name(),
                ProductStatus.ACTIVE.name(), new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.createProduct(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("PROD-04: createProduct with invalid productType → IllegalArgumentException")
    void createProduct_invalidProductType_throws() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        ProductRequest req = buildProductRequest("Test", "1", "INVALID_TYPE",
                ProductStatus.ACTIVE.name(), new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.createProduct(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_TYPE");
    }

    @Test
    @DisplayName("PROD-05: updateProduct changes core fields and returns updated response")
    void updateProduct_success() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        Product existingProduct = buildProduct(10L, category, "Old Name", ProductType.READY_MADE,
                ProductStatus.ACTIVE, new BigDecimal("50.00"));

        when(productRepository.findById(10L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(anyLong())).thenReturn(List.of());
        when(inventoryRepository.findByProductId(anyLong())).thenReturn(Optional.empty());
        when(productRelatedRepository.findByProductId(anyLong())).thenReturn(List.of());
        // Note: existsByProductId is NOT called when productType stays READY_MADE → READY_MADE
        // (the "!oldNeedsInventory && newNeedsInventory" branch is skipped)

        ProductRequest req = buildProductRequest("New Name", "1", ProductType.READY_MADE.name(),
                ProductStatus.ACTIVE.name(), new BigDecimal("75.00"));

        ProductDetailResponse response = service.updateProduct(10L, req);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getPrice()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("PROD-06: changeProductStatus to INACTIVE succeeds")
    void changeProductStatus_inactive_succeeds() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        Product product = buildProduct(10L, category, "Painting", ProductType.READY_MADE,
                ProductStatus.ACTIVE, new BigDecimal("50.00"));

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(anyLong())).thenReturn(List.of());
        when(inventoryRepository.findByProductId(anyLong())).thenReturn(Optional.empty());
        when(productRelatedRepository.findByProductId(anyLong())).thenReturn(List.of());

        ProductDetailResponse response = service.changeProductStatus(10L, "INACTIVE");

        assertThat(response.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("PROD-07: changeProductStatus for unknown product → ResourceNotFoundException")
    void changeProductStatus_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeProductStatus(99L, "INACTIVE"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // Related products tests
    // =========================================================================

    @Test
    @DisplayName("REL-01: replaceRelatedProducts clears existing and saves new set")
    void replaceRelatedProducts_success() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        Product source = buildProduct(1L, category, "Source", ProductType.READY_MADE,
                ProductStatus.ACTIVE, BigDecimal.ONE);
        Product related = buildProduct(2L, category, "Related", ProductType.READY_MADE,
                ProductStatus.ACTIVE, BigDecimal.ONE);

        when(productRepository.findById(1L)).thenReturn(Optional.of(source));
        when(productRepository.findById(2L)).thenReturn(Optional.of(related));
        when(productRelatedRepository.findByProductId(1L)).thenReturn(Collections.emptyList());
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(anyLong())).thenReturn(List.of());

        RelatedProductsRequest req = new RelatedProductsRequest();
        req.setProductIds(List.of(2L));

        List<ProductSummaryResponse> result = service.replaceRelatedProducts(1L, req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        verify(productRelatedRepository).saveAll(any());
    }

    @Test
    @DisplayName("REL-02: self-reference in related products → IllegalArgumentException")
    void replaceRelatedProducts_selfReference_throws() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        Product source = buildProduct(1L, category, "Source", ProductType.READY_MADE,
                ProductStatus.ACTIVE, BigDecimal.ONE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(source));

        RelatedProductsRequest req = new RelatedProductsRequest();
        req.setProductIds(List.of(1L));  // same ID as source — self-reference

        assertThatThrownBy(() -> service.replaceRelatedProducts(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itself");
    }

    @Test
    @DisplayName("REL-03: invalid related product ID → ResourceNotFoundException")
    void replaceRelatedProducts_invalidRelatedId_throws() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        Product source = buildProduct(1L, category, "Source", ProductType.READY_MADE,
                ProductStatus.ACTIVE, BigDecimal.ONE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(source));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        RelatedProductsRequest req = new RelatedProductsRequest();
        req.setProductIds(List.of(999L));

        assertThatThrownBy(() -> service.replaceRelatedProducts(1L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // =========================================================================
    // Inventory tests
    // =========================================================================

    @Test
    @DisplayName("INV-01: updateInventory sets the quantity on the existing inventory row")
    void updateInventory_success() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        Product product = buildProduct(10L, category, "Painting", ProductType.READY_MADE,
                ProductStatus.ACTIVE, new BigDecimal("50.00"));
        Inventory inventory = buildInventory(product, 5);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryUpdateRequest req = new InventoryUpdateRequest();
        req.setAvailableQuantity(25);

        service.updateInventory(10L, req);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantityOnHand()).isEqualTo(25);
    }

    @Test
    @DisplayName("INV-02: updateInventory on PORTFOLIO_ONLY product → InventoryTypeConflictException")
    void updateInventory_portfolioOnly_throws() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        Product product = buildProduct(10L, category, "Portfolio", ProductType.PORTFOLIO_ONLY,
                ProductStatus.ACTIVE, BigDecimal.ZERO);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        InventoryUpdateRequest req = new InventoryUpdateRequest();
        req.setAvailableQuantity(10);

        assertThatThrownBy(() -> service.updateInventory(10L, req))
                .isInstanceOf(com.handmadeart.ecommerce.exception.InventoryTypeConflictException.class)
                .hasMessageContaining("PORTFOLIO_ONLY");
    }

    @Test
    @DisplayName("INV-03: updateInventory for unknown product → ResourceNotFoundException")
    void updateInventory_notFound_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        InventoryUpdateRequest req = new InventoryUpdateRequest();
        req.setAvailableQuantity(10);

        assertThatThrownBy(() -> service.updateInventory(99L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // Product type transition tests
    // =========================================================================

    @Test
    @DisplayName("PROD-08: updateProduct READY_MADE → PORTFOLIO_ONLY removes inventory row")
    void updateProduct_typeTransitionToPortfolioOnly_deletesInventory() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        Product existingProduct = buildProduct(10L, category, "Painting", ProductType.READY_MADE,
                ProductStatus.ACTIVE, new BigDecimal("50.00"));

        when(productRepository.findById(10L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(anyLong())).thenReturn(List.of());
        when(productRelatedRepository.findByProductId(anyLong())).thenReturn(List.of());
        // Inventory row exists for the old READY_MADE type
        Inventory existingInventory = buildInventory(existingProduct, 5);
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(existingInventory));

        ProductRequest req = buildProductRequest("Painting", "1",
                ProductType.PORTFOLIO_ONLY.name(), ProductStatus.ACTIVE.name(), new BigDecimal("50.00"));

        service.updateProduct(10L, req);

        // Inventory row must be removed when type changes to PORTFOLIO_ONLY
        verify(inventoryRepository).delete(existingInventory);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("PROD-09: updateProduct PORTFOLIO_ONLY → READY_MADE creates inventory row")
    void updateProduct_typeTransitionFromPortfolioOnly_createsInventory() {
        Category category = buildCategory(1L, "Art", null, CategoryStatus.ACTIVE);
        Product existingProduct = buildProduct(10L, category, "Portfolio Piece", ProductType.PORTFOLIO_ONLY,
                ProductStatus.ACTIVE, BigDecimal.ZERO);

        when(productRepository.findById(10L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(anyLong())).thenReturn(List.of());
        when(productRelatedRepository.findByProductId(anyLong())).thenReturn(List.of());
        // No inventory row for the old PORTFOLIO_ONLY type
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.empty());
        when(inventoryRepository.existsByProductId(10L)).thenReturn(false);

        ProductRequest req = buildProductRequest("Painting", "1",
                ProductType.READY_MADE.name(), ProductStatus.ACTIVE.name(), new BigDecimal("50.00"));

        service.updateProduct(10L, req);

        // Inventory row must be created when type changes to READY_MADE
        verify(inventoryRepository).save(any(Inventory.class));
    }

    // =========================================================================
    // Image management tests
    // =========================================================================

    @Test
    @DisplayName("IMG-01: removeProductImage with imageId belonging to different product → ResourceNotFoundException")
    void removeProductImage_imageBelongsToDifferentProduct_throws() {
        // productId=1, but the image belongs to product 2
        when(productRepository.existsById(1L)).thenReturn(true);

        // Build a ProductImage that belongs to product 2, not product 1
        Category category = buildCategory(2L, "Art", null, CategoryStatus.ACTIVE);
        Product otherProduct = buildProduct(2L, category, "Other Product", ProductType.READY_MADE,
                ProductStatus.ACTIVE, new BigDecimal("30.00"));
        ProductImage image = new ProductImage();
        image.setProduct(otherProduct);
        image.setStorageReference("product-2/someimage.jpg");
        try {
            var f = ProductImage.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(image, 99L);
        } catch (Exception ignored) {}

        when(productImageRepository.findById(99L)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> service.removeProductImage(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found for this product");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Category buildCategory(Long id, String name, String description, CategoryStatus status) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(description);
        c.setStatus(status);
        try {
            var f = Category.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception ignored) {}
        return c;
    }

    private Product buildProduct(Long id, Category category, String name, ProductType type,
                                  ProductStatus status, BigDecimal price) {
        Product p = new Product();
        p.setCategory(category);
        p.setName(name);
        p.setProductType(type);
        p.setStatus(status);
        p.setPrice(price);
        try {
            var f = Product.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {}
        return p;
    }

    private Inventory buildInventory(Product product, int quantity) {
        Inventory inv = new Inventory();
        inv.setProduct(product);
        inv.setQuantityOnHand(quantity);
        return inv;
    }

    private ProductRequest buildProductRequest(String name, String categoryId,
                                                String productType, String status,
                                                BigDecimal price) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setCategoryId(Long.parseLong(categoryId));
        req.setProductType(productType);
        req.setStatus(status);
        req.setPrice(price);
        return req;
    }
}
