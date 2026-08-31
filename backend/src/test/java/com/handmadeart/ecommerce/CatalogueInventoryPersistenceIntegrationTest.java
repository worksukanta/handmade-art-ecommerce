package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.entity.Category;
import com.handmadeart.ecommerce.entity.CategoryStatus;
import com.handmadeart.ecommerce.entity.Inventory;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductImage;
import com.handmadeart.ecommerce.entity.ProductRelated;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import com.handmadeart.ecommerce.repository.CategoryRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.ProductImageRepository;
import com.handmadeart.ecommerce.repository.ProductRelatedRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Database integration tests for catalogue and inventory persistence.
 *
 * Verifies approved schema rules from Database Design &amp; ERD §3.3–3.6, §3.12, §7, §11, §16–17:
 *
 * Category:
 *  - persists successfully
 *  - unique name constraint is enforced
 *  - status persists correctly
 *
 * Product:
 *  - persists with category FK
 *  - BigDecimal price preserved with NUMERIC(10,2) precision
 *  - negative price rejected by CHECK constraint
 *  - status and product_type persist correctly
 *  - queries by status and type work
 *
 * ProductImage:
 *  - belongs to product
 *  - positive file_size_bytes constraint enforced
 *  - is_primary and display_order persist
 *  - cascade delete: product deletion removes images
 *
 * ProductRelated:
 *  - directional relationship persists
 *  - duplicate pair is rejected by composite PK
 *  - self-reference is rejected by CHECK constraint
 *
 * Inventory:
 *  - one inventory row per product
 *  - stock quantity persists
 *  - negative stock rejected by CHECK constraint
 *  - cascade delete: product deletion removes inventory
 *
 * ACTIVATION:
 *   Requires a running PostgreSQL instance. Excluded from default test run.
 *   Run with:
 *     mvn clean test -Dgroups=db-integration -Dspring.profiles.active=db-integration
 */
@Tag("db-integration")
@SpringBootTest
@ActiveProfiles("db-integration")
class CatalogueInventoryPersistenceIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductImageRepository productImageRepository;
    @Autowired private ProductRelatedRepository productRelatedRepository;
    @Autowired private InventoryRepository inventoryRepository;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Category savedCategory(String name) {
        Category c = new Category();
        c.setName(name);
        c.setStatus(CategoryStatus.ACTIVE);
        return categoryRepository.saveAndFlush(c);
    }

    private Product savedProduct(Category category, String name, ProductType type) {
        Product p = new Product();
        p.setCategory(category);
        p.setName(name);
        p.setPrice(new BigDecimal("99.99"));
        p.setProductType(type);
        p.setStatus(ProductStatus.ACTIVE);
        return productRepository.saveAndFlush(p);
    }

    // =========================================================================
    // Category tests
    // =========================================================================

    @Test
    @Transactional
    void category_canBePersisted() {
        Category c = savedCategory("Paintings");
        assertThat(c.getId()).isNotNull().isPositive();
        assertThat(c.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
        assertThat(c.getCreatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void category_nameUnique_rejectsDuplicate() {
        savedCategory("Sculptures");
        Category dup = new Category();
        dup.setName("Sculptures");
        dup.setStatus(CategoryStatus.ACTIVE);
        assertThatThrownBy(() -> categoryRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void category_findByStatus_returnsActiveCategories() {
        savedCategory("Prints_active");
        Category inactive = new Category();
        inactive.setName("Prints_inactive");
        inactive.setStatus(CategoryStatus.INACTIVE);
        categoryRepository.saveAndFlush(inactive);

        List<Category> active = categoryRepository.findByStatus(CategoryStatus.ACTIVE);
        assertThat(active).extracting(Category::getName).contains("Prints_active");
        assertThat(active).extracting(Category::getName).doesNotContain("Prints_inactive");
    }

    // =========================================================================
    // Product tests
    // =========================================================================

    @Test
    @Transactional
    void product_canBePersistedWithCategory() {
        Category cat = savedCategory("Digital Art");
        Product p = savedProduct(cat, "Neon Landscape", ProductType.READY_MADE);

        assertThat(p.getId()).isNotNull().isPositive();
        assertThat(p.getCategory().getId()).isEqualTo(cat.getId());
        assertThat(p.getProductType()).isEqualTo(ProductType.READY_MADE);
        assertThat(p.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(p.getCreatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void product_bigDecimalPrecision_isPreservedAtNumeric10_2() {
        Category cat = savedCategory("Ceramics");
        Product p = new Product();
        p.setCategory(cat);
        p.setName("Blue Vase");
        p.setPrice(new BigDecimal("1234.56"));
        p.setProductType(ProductType.READY_MADE);
        p.setStatus(ProductStatus.ACTIVE);
        Product saved = productRepository.saveAndFlush(p);

        Product reloaded = productRepository.findById(saved.getId()).orElseThrow();
        // BigDecimal scale from NUMERIC(10,2) should preserve exactly two decimal places.
        assertThat(reloaded.getPrice()).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    @Transactional
    void product_negativePrice_rejectedByCheckConstraint() {
        Category cat = savedCategory("Textiles_neg");
        Product p = new Product();
        p.setCategory(cat);
        p.setName("Bad Price Product");
        p.setPrice(new BigDecimal("-0.01"));
        p.setProductType(ProductType.READY_MADE);
        p.setStatus(ProductStatus.ACTIVE);
        assertThatThrownBy(() -> productRepository.saveAndFlush(p))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void product_findByStatusAndProductType_returnsCorrectSubset() {
        Category cat = savedCategory("Photography");
        savedProduct(cat, "Sunset Print", ProductType.READY_MADE);
        savedProduct(cat, "Custom Portrait", ProductType.CUSTOM_AVAILABLE);

        Product inactive = new Product();
        inactive.setCategory(cat);
        inactive.setName("Old Work");
        inactive.setPrice(BigDecimal.TEN);
        inactive.setProductType(ProductType.READY_MADE);
        inactive.setStatus(ProductStatus.INACTIVE);
        productRepository.saveAndFlush(inactive);

        List<Product> activeReadyMade = productRepository
                .findByStatusAndProductType(ProductStatus.ACTIVE, ProductType.READY_MADE);
        assertThat(activeReadyMade).extracting(Product::getName).contains("Sunset Print");
        assertThat(activeReadyMade).extracting(Product::getName).doesNotContain("Custom Portrait", "Old Work");
    }

    // =========================================================================
    // ProductImage tests
    // =========================================================================

    @Test
    @Transactional
    void productImage_canBePersistedForProduct() {
        Category cat = savedCategory("Watercolours");
        Product p = savedProduct(cat, "River Scene", ProductType.READY_MADE);

        ProductImage img = new ProductImage();
        img.setProduct(p);
        img.setStorageReference("/uploads/products/1/abc123.jpg");
        img.setContentType("image/jpeg");
        img.setFileSizeBytes(204800);
        img.setDisplayOrder(0);
        img.setPrimary(true);
        ProductImage saved = productImageRepository.saveAndFlush(img);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getProduct().getId()).isEqualTo(p.getId());
        assertThat(saved.isPrimary()).isTrue();
        assertThat(saved.getUploadedAt()).isNotNull();
    }

    @Test
    @Transactional
    void productImage_zeroFileSize_rejectedByCheckConstraint() {
        Category cat = savedCategory("Oils");
        Product p = savedProduct(cat, "Autumn Trees", ProductType.PORTFOLIO_ONLY);

        ProductImage img = new ProductImage();
        img.setProduct(p);
        img.setStorageReference("/uploads/products/2/bad.jpg");
        img.setContentType("image/jpeg");
        img.setFileSizeBytes(0);  // CHECK file_size_bytes > 0
        assertThatThrownBy(() -> productImageRepository.saveAndFlush(img))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void productImage_cascadeDeleteWithProduct() {
        Category cat = savedCategory("Etchings");
        Product p = savedProduct(cat, "Ancient Map", ProductType.PORTFOLIO_ONLY);

        ProductImage img = new ProductImage();
        img.setProduct(p);
        img.setStorageReference("/uploads/products/3/map.png");
        img.setContentType("image/png");
        img.setFileSizeBytes(51200);
        ProductImage saved = productImageRepository.saveAndFlush(img);
        Long imgId = saved.getId();
        Long productId = p.getId();

        // Flush all pending state to the DB and clear the persistence context BEFORE
        // deleting the parent. This is required because the managed ProductImage entity
        // in the session holds a reference to the managed Product. If the Product is
        // marked REMOVED while ProductImage is still MANAGED in the same session,
        // Hibernate throws TransientObjectException during the delete flush.
        // Clearing first detaches both entities; deleteById then reloads only the
        // Product from the DB, marks it REMOVED, and flushes — PostgreSQL ON DELETE
        // CASCADE (V3 FK: product_image.product_id REFERENCES product ON DELETE CASCADE)
        // removes the image row at the DB level.
        em.flush();
        em.clear();

        productRepository.deleteById(productId);
        productRepository.flush();

        // Clear again so the subsequent findById issues a fresh SELECT rather than
        // hitting any remaining persistence-context state.
        em.clear();

        assertThat(productImageRepository.findById(imgId)).isEmpty();
    }

    @Test
    @Transactional
    void productImage_findByProductId_ordersByDisplayOrder() {
        Category cat = savedCategory("Prints");
        Product p = savedProduct(cat, "City Skyline", ProductType.READY_MADE);

        for (int i = 2; i >= 0; i--) {
            ProductImage img = new ProductImage();
            img.setProduct(p);
            img.setStorageReference("/uploads/products/img" + i + ".jpg");
            img.setContentType("image/jpeg");
            img.setFileSizeBytes(1024);
            img.setDisplayOrder(i);
            productImageRepository.save(img);
        }
        productImageRepository.flush();

        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(p.getId());
        assertThat(images).hasSize(3);
        assertThat(images.get(0).getDisplayOrder()).isEqualTo(0);
        assertThat(images.get(2).getDisplayOrder()).isEqualTo(2);
    }

    // =========================================================================
    // ProductRelated tests
    // =========================================================================

    @Test
    @Transactional
    void productRelated_directionalRelationshipPersists() {
        Category cat = savedCategory("Mixed Media");
        Product a = savedProduct(cat, "Abstract A", ProductType.READY_MADE);
        Product b = savedProduct(cat, "Abstract B", ProductType.READY_MADE);

        ProductRelated rel = new ProductRelated(a, b);
        productRelatedRepository.saveAndFlush(rel);

        List<ProductRelated> related = productRelatedRepository.findByProductId(a.getId());
        assertThat(related).hasSize(1);
        assertThat(related.get(0).getRelatedProduct().getId()).isEqualTo(b.getId());
    }

    @Test
    @Transactional
    void productRelated_duplicatePair_rejectedByCompositePK() {
        Category cat = savedCategory("Pastel");
        Product a = savedProduct(cat, "Pastel A", ProductType.READY_MADE);
        Product b = savedProduct(cat, "Pastel B", ProductType.READY_MADE);

        productRelatedRepository.saveAndFlush(new ProductRelated(a, b));

        // Spring Data save() calls EntityManager.merge() when the entity already has a
        // non-null identifier (the ProductRelatedId is set in the constructor). merge()
        // semantics: if a row with that PK already exists Hibernate treats it as an update
        // — no second INSERT is issued so the PK constraint never fires.
        //
        // To verify the database PK constraint we must force an actual INSERT.
        // EntityManager.persist() on a new entity with the same composite key will
        // attempt a real INSERT and let the DB PK violation bubble up through flush().
        ProductRelated duplicate = new ProductRelated(a, b);
        assertThatThrownBy(() -> {
            em.persist(duplicate);
            em.flush();
        }).isInstanceOf(Exception.class)
          .satisfies(ex ->
              assertThat(ex).isInstanceOfAny(
                  DataIntegrityViolationException.class,
                  jakarta.persistence.PersistenceException.class
              )
          );
    }

    @Test
    @Transactional
    void productRelated_selfReference_rejectedByCheckConstraint() {
        Category cat = savedCategory("Ink");
        Product a = savedProduct(cat, "Ink Work", ProductType.PORTFOLIO_ONLY);

        // Manually set self-referencing IDs after persist (id assigned after save)
        // Use a direct save with matching IDs — the CHECK constraint must reject it.
        ProductRelated selfRel = new ProductRelated(a, a);
        assertThatThrownBy(() -> productRelatedRepository.saveAndFlush(selfRel))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // =========================================================================
    // Inventory tests
    // =========================================================================

    @Test
    @Transactional
    void inventory_canBePersistedForProduct() {
        Category cat = savedCategory("Sculptures");
        Product p = savedProduct(cat, "Stone Bowl", ProductType.READY_MADE);

        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setQuantityOnHand(10);
        Inventory saved = inventoryRepository.saveAndFlush(inv);

        assertThat(saved.getProductId()).isEqualTo(p.getId());
        assertThat(saved.getQuantityOnHand()).isEqualTo(10);
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void inventory_negativeStock_rejectedByCheckConstraint() {
        Category cat = savedCategory("Glass");
        Product p = savedProduct(cat, "Glass Orb", ProductType.READY_MADE);

        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setQuantityOnHand(-1);  // CHECK quantity_on_hand >= 0
        assertThatThrownBy(() -> inventoryRepository.saveAndFlush(inv))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void inventory_zeroStockAllowed() {
        Category cat = savedCategory("Fiber");
        Product p = savedProduct(cat, "Woven Basket", ProductType.READY_MADE);

        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setQuantityOnHand(0);
        Inventory saved = inventoryRepository.saveAndFlush(inv);
        assertThat(saved.getQuantityOnHand()).isEqualTo(0);
    }

    @Test
    @Transactional
    void inventory_findByProductId_returnsCorrectRow() {
        Category cat = savedCategory("Bronze");
        Product p = savedProduct(cat, "Bronze Figurine", ProductType.READY_MADE);

        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setQuantityOnHand(5);
        inventoryRepository.saveAndFlush(inv);

        Optional<Inventory> found = inventoryRepository.findByProductId(p.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getQuantityOnHand()).isEqualTo(5);
    }

    @Test
    @Transactional
    void inventory_cascadeDeleteWithProduct() {
        // This test verifies that the PostgreSQL ON DELETE CASCADE defined on
        // inventory.product_id → product.id (V3 migration) physically removes the
        // inventory row when the parent product row is deleted.
        //
        // Inventory uses a shared-PK / @MapsId mapping: product_id is both the
        // Inventory PK and the FK to Product. Hibernate's entity-lifecycle handling
        // for this pattern (loading, @Generated SELECT-after-UPDATE, flush ordering)
        // can interfere when deleting via the JPA entity API inside the same session.
        // To isolate the DB constraint from Hibernate entity-lifecycle complexity,
        // the parent deletion is performed via a native SQL DELETE. This directly
        // exercises the PostgreSQL ON DELETE CASCADE and proves the constraint without
        // any JPA merge/persist/remove interference.
        Category cat = savedCategory("Clay");
        Product p = savedProduct(cat, "Clay Pot", ProductType.READY_MADE);

        Inventory inv = new Inventory();
        inv.setProduct(p);
        inv.setQuantityOnHand(3);
        inventoryRepository.saveAndFlush(inv);
        Long productId = p.getId();

        // Flush and clear before deletion so no managed entities are present
        // in the persistence context during the native DELETE.
        em.flush();
        em.clear();

        // Delete the product row directly via native SQL — this is what the test
        // is actually verifying: that PostgreSQL's ON DELETE CASCADE removes the
        // inventory row when the product row is deleted, regardless of JPA lifecycle.
        em.createNativeQuery("DELETE FROM product WHERE id = :id")
                .setParameter("id", productId)
                .executeUpdate();
        em.flush();

        // Native count query: prove the inventory row is physically gone in PostgreSQL.
        // This bypasses all JPA entity-cache and repository semantics.
        Number inventoryCount = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM inventory WHERE product_id = :id")
                .setParameter("id", productId)
                .getSingleResult();
        assertThat(inventoryCount.longValue())
                .as("inventory row must be removed by PostgreSQL ON DELETE CASCADE after product deletion")
                .isEqualTo(0L);

        // Also confirm the product row is gone.
        Number productCount = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM product WHERE id = :id")
                .setParameter("id", productId)
                .getSingleResult();
        assertThat(productCount.longValue())
                .as("product row must be physically deleted")
                .isEqualTo(0L);
    }
}
