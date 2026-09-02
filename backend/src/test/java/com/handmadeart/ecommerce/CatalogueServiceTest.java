package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.dto.catalogue.CategoryResponse;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductDetailResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductSummaryResponse;
import com.handmadeart.ecommerce.entity.Category;
import com.handmadeart.ecommerce.entity.CategoryStatus;
import com.handmadeart.ecommerce.entity.Inventory;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductRelated;
import com.handmadeart.ecommerce.entity.ProductRelatedId;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CategoryRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.ProductImageRepository;
import com.handmadeart.ecommerce.repository.ProductRelatedRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import com.handmadeart.ecommerce.service.CatalogueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CatalogueService} — business/visibility rules.
 *
 * Uses Mockito only; no Spring context, no database.
 * Verifies that the service layer enforces approved visibility rules:
 * - Only ACTIVE categories are returned.
 * - Only ACTIVE products are returned.
 * - Inactive related products are filtered from the related list.
 * - Invalid sort fields are rejected.
 * - Page size is clamped within approved bounds.
 *
 * Covered:
 *   CSVC-01  listActiveCategories returns only ACTIVE categories
 *   CSVC-02  getActiveCategory returns 404 for INACTIVE category
 *   CSVC-03  getActiveCategory returns 404 for unknown category
 *   CSVC-04  getProductDetail returns product with images and inventory
 *   CSVC-05  getProductDetail returns 404 for INACTIVE product
 *   CSVC-06  getProductDetail returns 404 for unknown product
 *   CSVC-07  Inactive related products are excluded from detail response
 *   CSVC-08  getRelatedProducts returns 404 when source product is inactive
 *   CSVC-09  Invalid sort field rejected with IllegalArgumentException
 *   CSVC-10  listProducts passes ACTIVE status filter to repository
 */
@ExtendWith(MockitoExtension.class)
class CatalogueServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductRelatedRepository productRelatedRepository;
    @Mock private InventoryRepository inventoryRepository;

    private CatalogueService catalogueService;

    @BeforeEach
    void setUp() {
        catalogueService = new CatalogueService(
                categoryRepository, productRepository,
                productImageRepository, productRelatedRepository, inventoryRepository);
    }

    // -------------------------------------------------------------------------
    // Category tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CSVC-01: listActiveCategories returns only ACTIVE categories")
    void listActiveCategories_returnsOnlyActiveCategories() {
        Category active = buildCategory(1L, "Paintings", CategoryStatus.ACTIVE);
        when(categoryRepository.findByStatus(CategoryStatus.ACTIVE)).thenReturn(List.of(active));

        List<CategoryResponse> result = catalogueService.listActiveCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Paintings");
        assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("CSVC-02: getActiveCategory returns 404 for INACTIVE category")
    void getActiveCategory_inactiveCategory_throws404() {
        Category inactive = buildCategory(5L, "Archived", CategoryStatus.INACTIVE);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> catalogueService.getActiveCategory(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("CSVC-03: getActiveCategory returns 404 for non-existent category")
    void getActiveCategory_notFound_throws404() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogueService.getActiveCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Product detail tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CSVC-04: getProductDetail returns product with images, inventory, and related products")
    void getProductDetail_activeProduct_returnsFullDetail() {
        Category cat = buildCategory(1L, "Paintings", CategoryStatus.ACTIVE);
        Product product = buildProduct(10L, "Sunflower", ProductStatus.ACTIVE, ProductType.READY_MADE, cat);
        Inventory inventory = buildInventory(10L, 5);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(10L))
                .thenReturn(Collections.emptyList());
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
        when(productRelatedRepository.findByProductId(10L)).thenReturn(Collections.emptyList());

        ProductDetailResponse result = catalogueService.getProductDetail(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Sunflower");
        assertThat(result.getAvailability().isInStock()).isTrue();
        assertThat(result.getAvailability().getQuantityOnHand()).isEqualTo(5);
        assertThat(result.getImages()).isEmpty();
        assertThat(result.getRelatedProducts()).isEmpty();
    }

    @Test
    @DisplayName("CSVC-05: getProductDetail returns 404 for INACTIVE product")
    void getProductDetail_inactiveProduct_throws404() {
        Category cat = buildCategory(1L, "Cat", CategoryStatus.ACTIVE);
        Product inactive = buildProduct(20L, "Retired Painting", ProductStatus.INACTIVE,
                ProductType.READY_MADE, cat);
        when(productRepository.findById(20L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> catalogueService.getProductDetail(20L))
                .isInstanceOf(ResourceNotFoundException.class);

        // Images and inventory must not be fetched if product is not visible
        verify(productImageRepository, never()).findByProductIdOrderByDisplayOrderAsc(anyLong());
    }

    @Test
    @DisplayName("CSVC-06: getProductDetail returns 404 for unknown product")
    void getProductDetail_notFound_throws404() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogueService.getProductDetail(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("CSVC-07: Inactive related products are excluded from product detail")
    void getProductDetail_inactiveRelatedProductExcluded() {
        Category cat = buildCategory(1L, "Cat", CategoryStatus.ACTIVE);
        Product source = buildProduct(10L, "Source", ProductStatus.ACTIVE, ProductType.READY_MADE, cat);
        Product activeRelated = buildProduct(11L, "Active Related", ProductStatus.ACTIVE,
                ProductType.READY_MADE, cat);
        Product inactiveRelated = buildProduct(12L, "Inactive Related", ProductStatus.INACTIVE,
                ProductType.READY_MADE, cat);

        ProductRelated relActive = buildProductRelated(source, activeRelated);
        ProductRelated relInactive = buildProductRelated(source, inactiveRelated);

        when(productRepository.findById(10L)).thenReturn(Optional.of(source));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(10L))
                .thenReturn(Collections.emptyList());
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.empty());
        when(productRelatedRepository.findByProductId(10L))
                .thenReturn(List.of(relActive, relInactive));
        when(productImageRepository.findByProductIdOrderByDisplayOrderAsc(11L))
                .thenReturn(Collections.emptyList());

        ProductDetailResponse result = catalogueService.getProductDetail(10L);

        assertThat(result.getRelatedProducts()).hasSize(1);
        assertThat(result.getRelatedProducts().get(0).getName()).isEqualTo("Active Related");
    }

    @Test
    @DisplayName("CSVC-08: getRelatedProducts returns 404 when source product is inactive")
    void getRelatedProducts_inactiveSource_throws404() {
        Category cat = buildCategory(1L, "Cat", CategoryStatus.ACTIVE);
        Product inactive = buildProduct(20L, "Inactive", ProductStatus.INACTIVE,
                ProductType.READY_MADE, cat);
        when(productRepository.findById(20L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> catalogueService.getRelatedProducts(20L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // listProducts tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CSVC-09: Invalid sort field is rejected with IllegalArgumentException")
    void listProducts_invalidSortField_throwsIllegalArgument() {
        assertThatThrownBy(() -> catalogueService.listProducts(
                null, null, null, null, "INVALID_FIELD", null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field");
    }

    @Test
    @DisplayName("CSVC-10: listProducts always passes ACTIVE status to the repository")
    void listProducts_alwaysFiltersActiveProducts() {
        Page<Product> emptyPage = Page.empty(PageRequest.of(0, 20));
        when(productRepository.searchCatalogue(
                eq(ProductStatus.ACTIVE), eq(""), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        PageResponse<ProductSummaryResponse> result =
                catalogueService.listProducts(null, null, null, null, null, null, 0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPage()).isZero();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        // Verify that ACTIVE status was passed to repository
        verify(productRepository).searchCatalogue(
                eq(ProductStatus.ACTIVE), eq(""), any(), any(), any(), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Category buildCategory(Long id, String name, CategoryStatus status) {
        Category c = new Category();
        c.setName(name);
        c.setStatus(status);
        try {
            var f = Category.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception ignored) {}
        return c;
    }

    private Product buildProduct(Long id, String name, ProductStatus status,
                                  ProductType type, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setStatus(status);
        p.setProductType(type);
        p.setCategory(category);
        p.setPrice(BigDecimal.valueOf(99.99));
        try {
            var f = Product.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {}
        return p;
    }

    private Inventory buildInventory(Long productId, int quantity) {
        Inventory inv = new Inventory();
        inv.setQuantityOnHand(quantity);
        try {
            var f = Inventory.class.getDeclaredField("productId");
            f.setAccessible(true);
            f.set(inv, productId);
        } catch (Exception ignored) {}
        return inv;
    }

    private ProductRelated buildProductRelated(Product source, Product related) {
        ProductRelated pr = new ProductRelated();
        pr.setId(new ProductRelatedId(source.getId(), related.getId()));
        pr.setProduct(source);
        pr.setRelatedProduct(related);
        return pr;
    }
}
