package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.catalogue.CategoryResponse;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductDetailResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductSummaryResponse;
import com.handmadeart.ecommerce.entity.Category;
import com.handmadeart.ecommerce.entity.CategoryStatus;
import com.handmadeart.ecommerce.entity.Inventory;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductImage;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CategoryRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.ProductImageRepository;
import com.handmadeart.ecommerce.repository.ProductRelatedRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for public catalogue read operations.
 *
 * Enforces visibility rules:
 * - Only ACTIVE categories are returned through public endpoints.
 * - Only ACTIVE products are returned through public endpoints (REST API Spec §7).
 * - PORTFOLIO_ONLY products are returned in the public catalogue (they are ACTIVE and
 *   customer-visible display items; their availability reflects no purchaseability).
 * - Related products are filtered to ACTIVE status only — inactive products are
 *   never leaked through the related-products list (REST API Spec §7 note).
 *
 * All methods are read-only (@Transactional(readOnly = true)).
 */
@Service
@Transactional(readOnly = true)
public class CatalogueService {

    /**
     * Approved sort fields for the public product listing (REST API Spec §21).
     * Arbitrary client-controlled property sorting is rejected with 400.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name", "price", "created_at"
    );

    /** Default page size when client does not supply one. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** Maximum page size to prevent excessively large responses. */
    private static final int MAX_PAGE_SIZE = 100;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductRelatedRepository productRelatedRepository;
    private final InventoryRepository inventoryRepository;

    public CatalogueService(CategoryRepository categoryRepository,
                             ProductRepository productRepository,
                             ProductImageRepository productImageRepository,
                             ProductRelatedRepository productRelatedRepository,
                             InventoryRepository inventoryRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productRelatedRepository = productRelatedRepository;
        this.inventoryRepository = inventoryRepository;
    }

    // =========================================================================
    // Category operations
    // =========================================================================

    /**
     * List all ACTIVE categories for public catalogue navigation.
     *
     * @return list of active categories (may be empty)
     */
    public List<CategoryResponse> listActiveCategories() {
        return categoryRepository.findByStatus(CategoryStatus.ACTIVE)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * Retrieve a single category by ID, visible to the public only if ACTIVE.
     *
     * @param id category ID
     * @return CategoryResponse for an ACTIVE category
     * @throws ResourceNotFoundException if the category does not exist or is not ACTIVE
     */
    public CategoryResponse getActiveCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found"));

        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw new ResourceNotFoundException("Category not found");
        }

        return CategoryResponse.from(category);
    }

    // =========================================================================
    // Product listing
    // =========================================================================

    /**
     * List customer-visible (ACTIVE) products with optional filtering, sorting,
     * and pagination.
     *
     * Visibility: ACTIVE products only.
     * Sorting: restricted to approved fields (name, price, created_at). 400 if invalid.
     * Pagination: zero-based page index; default size 20; max size 100.
     *
     * @param q          optional free-text search (name or description)
     * @param categoryId optional category filter
     * @param minPrice   optional minimum price (inclusive)
     * @param maxPrice   optional maximum price (inclusive)
     * @param sort       optional sort field (approved: name, price, created_at)
     * @param direction  sort direction (ASC or DESC), defaults to ASC
     * @param page       zero-based page number (default 0)
     * @param size       page size (default 20, max 100)
     * @return paged ProductSummaryResponse
     * @throws IllegalArgumentException if sort field is not in the approved set
     */
    public PageResponse<ProductSummaryResponse> listProducts(
            String q,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sort,
            String direction,
            int page,
            int size) {

        // Validate and clamp page size
        if (size <= 0) size = DEFAULT_PAGE_SIZE;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        if (page < 0) page = 0;

        // Build sort — validate against approved whitelist
        Sort sortSpec = buildSort(sort, direction);
        Pageable pageable = PageRequest.of(page, size, sortSpec);

        // Use a typed empty string for an omitted search. PostgreSQL cannot infer the
        // type of a null parameter used first in the JPQL `:q IS NULL` predicate.
        String searchQ = (q != null && !q.isBlank()) ? q.strip() : "";

        Page<Product> productPage = productRepository.searchCatalogue(
                ProductStatus.ACTIVE,
                searchQ,
                categoryId,
                minPrice,
                maxPrice,
                pageable);

        // Map each product to a summary — fetch images per product (N+1 acceptable for
        // read-only catalogue; no eager loading to avoid Cartesian join with pagination)
        Page<ProductSummaryResponse> summaryPage = productPage.map(product -> {
            List<ProductImage> images = productImageRepository
                    .findByProductIdOrderByDisplayOrderAsc(product.getId());
            return ProductSummaryResponse.from(product, images);
        });

        return PageResponse.from(summaryPage);
    }

    // =========================================================================
    // Product detail
    // =========================================================================

    /**
     * Return full product detail for an ACTIVE product.
     *
     * Includes:
     * - category reference
     * - product images (ordered by display_order)
     * - availability (inventory data; null for PORTFOLIO_ONLY)
     * - related products (ACTIVE only, filtered on retrieval)
     *
     * @param id product ID
     * @return ProductDetailResponse
     * @throws ResourceNotFoundException if the product does not exist or is not ACTIVE
     */
    public ProductDetailResponse getProductDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException("Product not found");
        }

        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(id);

        Optional<Inventory> inventory = inventoryRepository.findByProductId(id);

        List<ProductSummaryResponse> relatedProducts = buildRelatedProducts(id);

        return ProductDetailResponse.from(product, images, inventory.orElse(null), relatedProducts);
    }

    /**
     * Return ACTIVE related products for a product.
     *
     * The source product must be ACTIVE. Related products that are not ACTIVE
     * are filtered out — they must not be leaked to customers.
     *
     * @param id source product ID
     * @return list of related ACTIVE product summaries (may be empty)
     * @throws ResourceNotFoundException if the source product does not exist or is not ACTIVE
     */
    public List<ProductSummaryResponse> getRelatedProducts(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException("Product not found");
        }

        return buildRelatedProducts(id);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Build the related-product summary list for a source product ID.
     * Only ACTIVE related products are included.
     */
    private List<ProductSummaryResponse> buildRelatedProducts(Long sourceProductId) {
        return productRelatedRepository.findByProductId(sourceProductId)
                .stream()
                .map(pr -> pr.getRelatedProduct())
                .filter(rp -> rp.getStatus() == ProductStatus.ACTIVE)
                .map(rp -> {
                    List<ProductImage> imgs = productImageRepository
                            .findByProductIdOrderByDisplayOrderAsc(rp.getId());
                    return ProductSummaryResponse.from(rp, imgs);
                })
                .toList();
    }

    /**
     * Build a Spring Data Sort from approved sort field + direction.
     *
     * @param sortField  one of: name, price, created_at (null defaults to created_at DESC)
     * @param direction  ASC or DESC (case-insensitive; null defaults to ASC for named fields,
     *                   DESC for the default created_at sort)
     * @throws IllegalArgumentException if sortField is not in the approved whitelist
     */
    private Sort buildSort(String sortField, String direction) {
        if (sortField == null || sortField.isBlank()) {
            // Default: newest first
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String normalised = sortField.strip().toLowerCase();
        if (!ALLOWED_SORT_FIELDS.contains(normalised)) {
            throw new IllegalArgumentException(
                    "Invalid sort field '" + sortField + "'. Allowed: name, price, created_at");
        }

        // Map API field name → JPA entity field name
        String entityField = switch (normalised) {
            case "name"       -> "name";
            case "price"      -> "price";
            case "created_at" -> "createdAt";
            default -> throw new IllegalArgumentException("Unexpected sort field: " + normalised);
        };

        Sort.Direction dir = Sort.Direction.ASC;
        if (direction != null && direction.strip().equalsIgnoreCase("DESC")) {
            dir = Sort.Direction.DESC;
        }

        return Sort.by(dir, entityField);
    }
}
