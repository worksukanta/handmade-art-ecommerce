package com.handmadeart.ecommerce.service;

import com.handmadeart.ecommerce.dto.admin.AdminProductDetailResponse;
import com.handmadeart.ecommerce.dto.admin.AdminProductSummaryResponse;
import com.handmadeart.ecommerce.dto.admin.CategoryRequest;
import com.handmadeart.ecommerce.dto.admin.InventoryResponse;
import com.handmadeart.ecommerce.dto.admin.InventoryUpdateRequest;
import com.handmadeart.ecommerce.dto.admin.ProductRequest;
import com.handmadeart.ecommerce.dto.admin.RelatedProductsRequest;
import com.handmadeart.ecommerce.dto.catalogue.CategoryResponse;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductDetailResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductImageResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductSummaryResponse;
import com.handmadeart.ecommerce.entity.Category;
import com.handmadeart.ecommerce.entity.CategoryStatus;
import com.handmadeart.ecommerce.entity.Inventory;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductImage;
import com.handmadeart.ecommerce.entity.ProductRelated;
import com.handmadeart.ecommerce.entity.ProductRelatedId;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import com.handmadeart.ecommerce.exception.DuplicateCategoryNameException;
import com.handmadeart.ecommerce.exception.InventoryTypeConflictException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CategoryRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.ProductImageRepository;
import com.handmadeart.ecommerce.repository.ProductRelatedRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for ADMIN catalogue management operations.
 *
 * Authorization enforcement note:
 *   Route-level {@code hasRole("ADMIN")} is enforced by SecurityConfig for all
 *   /api/v1/admin/** paths. These service methods therefore trust they are only
 *   called for authenticated ADMIN principals.
 *
 * Design:
 *   - Category create/update: duplicate name check → 409.
 *   - Category status change: ACTIVE↔INACTIVE controlled transitions.
 *   - Product create: validates category reference; sets explicit status and type.
 *   - Product update: validates category reference; does not change type arbitrarily.
 *   - Product status: ACTIVE↔INACTIVE only.
 *   - Admin product listing: all statuses visible (distinct from public /api/v1/products).
 *   - Image upload: multipart file saved to local filesystem; metadata persisted.
 *     DEC-003 (file type/size limits) remains OPEN — content-type validated as image/*.
 *   - Related products: full replacement of the directional set for a source product.
 *     Self-reference rejected. Invalid product IDs rejected (404).
 *   - Inventory: non-negative quantity only; only READY_MADE / CUSTOM_AVAILABLE
 *     products may have inventory rows.
 *
 * DEC-009 (inventory concurrency strategy) remains OPEN.
 * No pessimistic/optimistic locking is introduced here.
 */
@Service
@Transactional
public class AdminCatalogueService {

    private static final Logger log = LoggerFactory.getLogger(AdminCatalogueService.class);

    /** Default page size for admin listings. */
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    /** Allowed MIME type prefix for product images. */
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";

    /** Base directory for product image uploads. Externalized for flexibility. */
    private final Path uploadRoot;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductRelatedRepository productRelatedRepository;
    private final InventoryRepository inventoryRepository;

    public AdminCatalogueService(
            @Value("${app.upload.product-images:uploads/product-images}") String uploadDir,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ProductRelatedRepository productRelatedRepository,
            InventoryRepository inventoryRepository) {

        this.uploadRoot = Paths.get(uploadDir);
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productRelatedRepository = productRelatedRepository;
        this.inventoryRepository = inventoryRepository;
    }

    // =========================================================================
    // Category management
    // =========================================================================

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * Create a new category.
     *
     * @param request validated category name + description
     * @return CategoryResponse for the persisted category
     * @throws DuplicateCategoryNameException if name is already in use
     */
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.getName().strip();
        if (categoryRepository.existsByName(name)) {
            throw new DuplicateCategoryNameException(
                    "A category named '" + name + "' already exists");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(request.getDescription());
        category.setStatus(CategoryStatus.ACTIVE);

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    /**
     * Update an existing category's name and/or description.
     *
     * @param id      category ID
     * @param request updated name + description
     * @return CategoryResponse for the updated category
     * @throws ResourceNotFoundException      if category not found
     * @throws DuplicateCategoryNameException if the new name is already taken by another category
     */
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String newName = request.getName().strip();

        // Allow rename to the same name (no-op), but reject if another category has the name
        if (!newName.equalsIgnoreCase(category.getName()) && categoryRepository.existsByName(newName)) {
            throw new DuplicateCategoryNameException(
                    "A category named '" + newName + "' already exists");
        }

        category.setName(newName);
        category.setDescription(request.getDescription());

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    /**
     * Activate or deactivate a category.
     *
     * Accepts status values: ACTIVE, INACTIVE (case-insensitive).
     *
     * @param id     category ID
     * @param status new status string
     * @return CategoryResponse for the updated category
     * @throws ResourceNotFoundException if category not found
     * @throws IllegalArgumentException  if status value is not valid
     */
    public CategoryResponse changeCategoryStatus(Long id, String status) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        CategoryStatus newStatus = parseCategoryStatus(status);
        category.setStatus(newStatus);

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    // =========================================================================
    // Admin product listing
    // =========================================================================

    /**
     * List all products for admin management — includes ALL statuses (ACTIVE and INACTIVE).
     *
     * This is intentionally separate from the public GET /api/v1/products which returns
     * only ACTIVE products (REST API Spec §16 Design Note).
     *
     * @param page zero-based page number
     * @param size page size (clamped to 1–100)
     * @return paged ProductSummaryResponse for all products
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminProductSummaryResponse> listAllProducts(int page, int size) {
        if (size <= 0) size = DEFAULT_PAGE_SIZE;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        if (page < 0) page = 0;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findAll(pageable);

        Page<AdminProductSummaryResponse> summaryPage = productPage.map(product -> {
            List<ProductImage> images = productImageRepository
                    .findByProductIdOrderByDisplayOrderAsc(product.getId());
            return AdminProductSummaryResponse.from(product, images);
        });

        return PageResponse.from(summaryPage);
    }

    // =========================================================================
    // Product management
    // =========================================================================

    @Transactional(readOnly = true)
    public AdminProductDetailResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return buildAdminProductDetailResponse(product);
    }

    /**
     * Create a new product.
     *
     * Validates:
     * - categoryId references an existing category
     * - productType is a valid approved value
     * - status is a valid approved value
     * - price >= 0
     *
     * @param request validated product fields
     * @return ProductDetailResponse for the persisted product
     * @throws ResourceNotFoundException if categoryId does not reference an existing category
     * @throws IllegalArgumentException  if productType or status is invalid
     */
    public ProductDetailResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.getCategoryId()));

        ProductType productType = parseProductType(request.getProductType());
        ProductStatus status = parseProductStatus(request.getStatus());

        Product product = new Product();
        product.setCategory(category);
        product.setName(request.getName().strip());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setProductType(productType);
        product.setStatus(status);

        Product saved = productRepository.save(product);

        // Create an inventory row for types that require stock tracking
        if (productType == ProductType.READY_MADE || productType == ProductType.CUSTOM_AVAILABLE) {
            Inventory inventory = new Inventory();
            inventory.setProduct(saved);
            inventoryRepository.save(inventory);
        }

        return buildProductDetailResponse(saved);
    }

    /**
     * Update core product data.
     *
     * Note: changing productType is allowed by this endpoint per the spec
     * (ProductRequest contains productType). If changing from/to PORTFOLIO_ONLY
     * the inventory row will be created or deleted accordingly.
     *
     * @param id      product ID
     * @param request updated product fields
     * @return ProductDetailResponse for the updated product
     * @throws ResourceNotFoundException if product or category not found
     * @throws IllegalArgumentException  if productType or status is invalid
     */
    public ProductDetailResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.getCategoryId()));

        ProductType newType = parseProductType(request.getProductType());
        ProductType oldType = product.getProductType();

        product.setCategory(category);
        product.setName(request.getName().strip());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setProductType(newType);
        product.setStatus(parseProductStatus(request.getStatus()));

        Product saved = productRepository.save(product);

        // Handle inventory row lifecycle when product type changes
        boolean oldNeedsInventory = oldType == ProductType.READY_MADE || oldType == ProductType.CUSTOM_AVAILABLE;
        boolean newNeedsInventory = newType == ProductType.READY_MADE || newType == ProductType.CUSTOM_AVAILABLE;

        if (!oldNeedsInventory && newNeedsInventory) {
            // Gained inventory requirement — create row if missing
            if (!inventoryRepository.existsByProductId(id)) {
                Inventory inventory = new Inventory();
                inventory.setProduct(saved);
                inventoryRepository.save(inventory);
            }
        } else if (oldNeedsInventory && !newNeedsInventory) {
            // Lost inventory requirement — remove row
            inventoryRepository.findByProductId(id).ifPresent(inventoryRepository::delete);
        }

        return buildProductDetailResponse(saved);
    }

    /**
     * Change a product's status (ACTIVE / INACTIVE).
     *
     * @param id     product ID
     * @param status new status string
     * @return ProductDetailResponse for the updated product
     * @throws ResourceNotFoundException if product not found
     * @throws IllegalArgumentException  if status value is not valid
     */
    public ProductDetailResponse changeProductStatus(Long id, String status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setStatus(parseProductStatus(status));
        Product saved = productRepository.save(product);
        return buildProductDetailResponse(saved);
    }

    // =========================================================================
    // Product image management
    // =========================================================================

    /**
     * Accept a multipart image upload, save the file to local filesystem storage,
     * and persist the metadata as a ProductImage record.
     *
     * Authorization: ADMIN only (enforced by SecurityConfig).
     *
     * DEC-003 (file type/size limits) is OPEN. This implementation:
     * - Validates content type is image/* (rejects non-image types → 400).
     * - Does NOT enforce a specific size limit (DEC-003 must be resolved first).
     *   Spring Boot's default multipart limits apply (typically 1MB/10MB).
     * - Stores a server-generated UUID filename to prevent path-traversal attacks.
     * - Returns metadata only; never exposes physical filesystem paths in responses.
     *
     * @param productId product to attach the image to
     * @param file      uploaded multipart file
     * @return ProductImageResponse for the persisted record
     * @throws ResourceNotFoundException if product not found
     * @throws IllegalArgumentException  if file is empty or content type is not image/*
     */
    public ProductImageResponse addProductImage(Long productId, MultipartFile file) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
            throw new IllegalArgumentException(
                    "Only image files are accepted (content type must start with 'image/')");
        }

        // Derive extension from content type safely (never use original filename)
        String extension = extensionForContentType(contentType);

        // Generate a UUID-based server filename — never derived from the client-supplied name
        String serverFilename = UUID.randomUUID() + extension;
        Path productDir = uploadRoot.resolve("product-" + productId);

        try {
            Files.createDirectories(productDir);
            Path destination = productDir.resolve(serverFilename);
            file.transferTo(destination.toFile());
        } catch (IOException ex) {
            log.error("Failed to store product image for product {}", productId, ex);
            throw new RuntimeException("Image storage failed");
        }

        // Storage reference is a logical path — never expose raw filesystem root
        String storageReference = "product-" + productId + "/" + serverFilename;

        // Single DB call for both isPrimary and displayOrder — count before this save
        long existingCount = productImageRepository.countByProductId(productId);
        boolean isPrimary = existingCount == 0;
        int displayOrder = (int) existingCount;

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setStorageReference(storageReference);
        image.setOriginalFilename(file.getOriginalFilename());
        image.setContentType(contentType);
        image.setFileSizeBytes(Math.toIntExact(file.getSize()));
        image.setDisplayOrder(displayOrder);
        image.setPrimary(isPrimary);

        ProductImage saved = productImageRepository.save(image);
        return ProductImageResponse.from(saved);
    }

    /**
     * Remove a product image record and its associated stored file.
     *
     * @param productId product ID (validates image belongs to this product)
     * @param imageId   image ID to remove
     * @throws ResourceNotFoundException if product or image not found,
     *                                   or if image does not belong to the product
     */
    public void removeProductImage(Long productId, Long imageId) {
        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found"));

        if (!image.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Product image not found for this product");
        }

        // Best-effort file deletion — log but don't fail if file is already gone
        try {
            Path file = uploadRoot.resolve(image.getStorageReference());
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.warn("Could not delete image file '{}': {}", image.getStorageReference(), ex.getMessage());
        }

        productImageRepository.delete(image);
    }

    // =========================================================================
    // Related products management
    // =========================================================================

    /**
     * Replace the related-product set for a source product.
     *
     * The spec (tbl[27]) defines this as a PUT (full replacement):
     * - All existing directional relationships for the source product are cleared.
     * - New ones are created from the supplied productIds list.
     *
     * Validation:
     * - sourceId must exist.
     * - No self-references (sourceId in productIds → 400).
     * - Each productId in the list must exist (→ 400/404).
     *
     * Directional semantics: this only manages the A→B direction for product A.
     * Reverse B→A relationships are NOT automatically created.
     *
     * @param sourceProductId product whose related set is being replaced
     * @param request         list of product IDs to set as related
     * @return list of ProductSummaryResponse for the new related products
     * @throws ResourceNotFoundException if sourceProductId or any related product ID not found
     * @throws IllegalArgumentException  if self-reference is attempted
     */
    public List<ProductSummaryResponse> replaceRelatedProducts(
            Long sourceProductId, RelatedProductsRequest request) {

        Product sourceProduct = productRepository.findById(sourceProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<Long> relatedIds = request.getProductIds();

        // Self-reference check
        if (relatedIds.contains(sourceProductId)) {
            throw new IllegalArgumentException(
                    "A product cannot be related to itself");
        }

        // Validate all related product IDs exist
        List<Product> relatedProducts = relatedIds.stream()
                .map(relId -> productRepository.findById(relId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Related product not found: " + relId)))
                .toList();

        // Clear existing directional relationships for this source product
        List<ProductRelated> existing = productRelatedRepository.findByProductId(sourceProductId);
        productRelatedRepository.deleteAll(existing);

        // Create the new set
        List<ProductRelated> newRelations = relatedProducts.stream()
                .map(relProduct -> new ProductRelated(sourceProduct, relProduct))
                .toList();
        productRelatedRepository.saveAll(newRelations);

        // Return summary responses (include all statuses — admin view)
        return relatedProducts.stream()
                .map(rp -> {
                    List<ProductImage> imgs = productImageRepository
                            .findByProductIdOrderByDisplayOrderAsc(rp.getId());
                    return ProductSummaryResponse.from(rp, imgs);
                })
                .toList();
    }

    // =========================================================================
    // Inventory management
    // =========================================================================

    /**
     * List all inventory records, paginated.
     *
     * Only products with inventory rows are included (READY_MADE and CUSTOM_AVAILABLE).
     * PORTFOLIO_ONLY products have no inventory row and are not shown here.
     *
     * @param page zero-based page number
     * @param size page size (clamped to 1–100)
     * @return paged InventoryResponse
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> listInventory(int page, int size) {
        if (size <= 0) size = DEFAULT_PAGE_SIZE;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        if (page < 0) page = 0;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "productId"));
        Page<Inventory> inventoryPage = inventoryRepository.findAll(pageable);
        return PageResponse.from(inventoryPage.map(InventoryResponse::from));
    }

    /**
     * Get inventory for a specific product.
     *
     * @param productId product ID
     * @return InventoryResponse
     * @throws ResourceNotFoundException if product not found or has no inventory row
     *                                   (i.e. PORTFOLIO_ONLY products)
     */
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record for product " + productId
                        + " (PORTFOLIO_ONLY products do not have inventory)"));

        return InventoryResponse.from(inventory);
    }

    /**
     * Update available stock for a ready-made or custom-available product.
     *
     * Rules:
     * - Product must exist.
     * - Product must have an inventory row (PORTFOLIO_ONLY products → 409).
     * - Quantity must be >= 0 (validated at DTO level; DB also enforces this).
     *
     * DEC-009 (inventory concurrency strategy) is OPEN.
     * No locking is applied here — this is basic admin stock management only.
     *
     * @param productId product ID
     * @param request   new quantity
     * @return InventoryResponse for the updated record
     * @throws ResourceNotFoundException if product not found
     * @throws IllegalStateException     if product type does not support inventory
     *                                   (PORTFOLIO_ONLY → 409)
     */
    public InventoryResponse updateInventory(Long productId, InventoryUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // PORTFOLIO_ONLY products do not have inventory rows
        if (product.getProductType() == ProductType.PORTFOLIO_ONLY) {
            throw new InventoryTypeConflictException(
                    "PORTFOLIO_ONLY products do not support inventory management");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    // Create inventory row if somehow missing (e.g. legacy data)
                    Inventory newInventory = new Inventory();
                    newInventory.setProduct(product);
                    return newInventory;
                });

        inventory.setQuantityOnHand(request.getAvailableQuantity());
        Inventory saved = inventoryRepository.save(inventory);
        return InventoryResponse.from(saved);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Build a full ProductDetailResponse for a product, loading images and inventory.
     * Related products are fetched from the persistent set (no status filter for admin).
     */
    private ProductDetailResponse buildProductDetailResponse(Product product) {
        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(product.getId());
        Optional<Inventory> inventory = inventoryRepository.findByProductId(product.getId());

        // Admin view of related products — include all statuses
        List<ProductSummaryResponse> relatedSummaries =
                productRelatedRepository.findByProductId(product.getId())
                        .stream()
                        .map(pr -> {
                            Product rp = pr.getRelatedProduct();
                            List<ProductImage> imgs = productImageRepository
                                    .findByProductIdOrderByDisplayOrderAsc(rp.getId());
                            return ProductSummaryResponse.from(rp, imgs);
                        })
                        .toList();

        return ProductDetailResponse.from(product, images, inventory.orElse(null), relatedSummaries);
    }

    private AdminProductDetailResponse buildAdminProductDetailResponse(Product product) {
        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(product.getId());
        Optional<Inventory> inventory = inventoryRepository.findByProductId(product.getId());
        List<AdminProductSummaryResponse> relatedSummaries =
                productRelatedRepository.findByProductId(product.getId()).stream()
                        .map(related -> {
                            Product relatedProduct = related.getRelatedProduct();
                            List<ProductImage> relatedImages = productImageRepository
                                    .findByProductIdOrderByDisplayOrderAsc(relatedProduct.getId());
                            return AdminProductSummaryResponse.from(relatedProduct, relatedImages);
                        }).toList();
        return AdminProductDetailResponse.from(product, images, inventory.orElse(null), relatedSummaries);
    }

    private static CategoryStatus parseCategoryStatus(String value) {
        try {
            return CategoryStatus.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid category status '" + value + "'. Allowed: ACTIVE, INACTIVE");
        }
    }

    private static ProductStatus parseProductStatus(String value) {
        try {
            return ProductStatus.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid product status '" + value + "'. Allowed: ACTIVE, INACTIVE");
        }
    }

    private static ProductType parseProductType(String value) {
        try {
            return ProductType.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid product type '" + value
                    + "'. Allowed: READY_MADE, CUSTOM_AVAILABLE, PORTFOLIO_ONLY");
        }
    }

    /**
     * Derive a safe file extension from a validated content type.
     * Returns an empty string for unrecognised subtypes.
     */
    private static String extensionForContentType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png"               -> ".png";
            case "image/gif"               -> ".gif";
            case "image/webp"              -> ".webp";
            default                        -> "";
        };
    }
}
