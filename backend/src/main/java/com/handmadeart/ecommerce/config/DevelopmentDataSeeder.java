package com.handmadeart.ecommerce.config;

import com.handmadeart.ecommerce.entity.*;
import com.handmadeart.ecommerce.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/** Explicitly enabled, idempotent local-development sample data. */
@Component
public class DevelopmentDataSeeder implements ApplicationRunner {

    static final String PRODUCT_A = "Hand-Painted Botanical Denim Jacket";
    static final String PRODUCT_B = "Hand-Painted Midnight Floral Shirt";
    static final String PRODUCT_C = "Custom Watercolour Family Portrait";
    static final String PRODUCT_D = "Golden Hour Heritage Portrait";

    private final boolean enabled;
    private final String adminEmail;
    private final String adminPassword;
    private final Path imageRoot;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRelatedRepository relatedRepository;
    private final ProductImageRepository imageRepository;

    public DevelopmentDataSeeder(
            @Value("${app.seed.enabled:false}") boolean enabled,
            @Value("${app.seed.admin-email:}") String adminEmail,
            @Value("${app.seed.admin-password:}") String adminPassword,
            @Value("${app.upload.product-images:uploads/product-images}") String imageRoot,
            PasswordEncoder passwordEncoder,
            AppUserRepository userRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            ProductRelatedRepository relatedRepository,
            ProductImageRepository imageRepository) {
        this.enabled = enabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.imageRoot = Paths.get(imageRoot).toAbsolutePath().normalize();
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.relatedRepository = relatedRepository;
        this.imageRepository = imageRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (!enabled) return;
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("APP_SEED_ADMIN_EMAIL and APP_SEED_ADMIN_PASSWORD are required when seeding is enabled");
        }

        ensureAdmin();
        Category clothing = ensureCategory("Painted Clothing", "Hand-painted wearable artwork and individually decorated clothing.");
        Category portraits = ensureCategory("Portraits", "Original portraits and commissioned portrait artwork.");
        ensureCategory("Handmade Accessories", "Small-batch handmade accessories and decorative pieces.");

        Product productA = ensureProduct(PRODUCT_A, "A one-of-a-kind denim jacket painted by hand with layered wildflowers and botanical details.", "149.00", clothing, ProductType.READY_MADE);
        Product productB = ensureProduct(PRODUCT_B, "A dark cotton shirt individually painted with a vivid floral pattern and fine brushwork.", "89.50", clothing, ProductType.READY_MADE);
        Product productC = ensureProduct(PRODUCT_C, "A personalised family portrait painted from customer-provided reference photographs.", "220.00", portraits, ProductType.CUSTOM_AVAILABLE);
        ensureProduct(PRODUCT_D, "A completed commissioned portrait displayed as an example of the artist's previous work.", "0.00", portraits, ProductType.PORTFOLIO_ONLY);

        ensureInventory(productA, 5);
        ensureInventory(productB, 1);
        ensureInventory(productC, 0);
        ensureRelated(productA, productB);
        ensureImage(productA, "seed-botanical-jacket.png", "Botanical Jacket", new Color(75, 118, 82), true);
        ensureImage(productB, "seed-floral-shirt.png", "Floral Shirt", new Color(67, 58, 92), true);
        ensureImage(productC, "seed-portrait-sample.png", "Portrait Sample", new Color(167, 103, 71), true);
    }

    private void ensureAdmin() {
        String email = adminEmail.strip().toLowerCase(Locale.ROOT);
        var existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() != UserRole.ADMIN) {
                throw new IllegalStateException("Configured seed admin email belongs to a non-ADMIN account");
            }
            return;
        }
        AppUser admin = new AppUser();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setFullName("Development Administrator");
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
    }

    private Category ensureCategory(String name, String description) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            category.setStatus(CategoryStatus.ACTIVE);
            return categoryRepository.save(category);
        });
    }

    private Product ensureProduct(String name, String description, String price, Category category, ProductType type) {
        return productRepository.findAll().stream().filter(product -> name.equals(product.getName())).findFirst().orElseGet(() -> {
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(new BigDecimal(price));
            product.setCategory(category);
            product.setProductType(type);
            product.setStatus(ProductStatus.ACTIVE);
            return productRepository.save(product);
        });
    }

    private void ensureInventory(Product product, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElse(null);
        if (inventory == null) {
            Inventory created = new Inventory();
            created.setProduct(product);
            created.setQuantityOnHand(quantity);
            inventoryRepository.save(created);
            return;
        }
        if (!Integer.valueOf(quantity).equals(inventory.getQuantityOnHand())) {
            inventory.setQuantityOnHand(quantity);
            inventoryRepository.save(inventory);
        }
    }

    private void ensureRelated(Product product, Product related) {
        ProductRelatedId id = new ProductRelatedId(product.getId(), related.getId());
        if (!relatedRepository.existsById(id)) relatedRepository.save(new ProductRelated(product, related));
    }

    private void ensureImage(Product product, String filename, String label, Color color, boolean primary) throws IOException {
        List<ProductImage> existing = imageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        if (existing.stream().anyMatch(image -> filename.equals(image.getOriginalFilename()))) return;

        Path productDirectory = imageRoot.resolve("product-" + product.getId()).normalize();
        if (!productDirectory.startsWith(imageRoot)) throw new IOException("Invalid seed image storage path");
        Files.createDirectories(productDirectory);
        Path file = productDirectory.resolve(filename);
        writePlaceholder(file, label, color);

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setStorageReference(imageRoot.relativize(file).toString().replace('\\', '/'));
        image.setOriginalFilename(filename);
        image.setContentType("image/png");
        image.setFileSizeBytes(Math.toIntExact(Files.size(file)));
        image.setDisplayOrder(0);
        image.setPrimary(primary);
        imageRepository.save(image);
    }

    private void writePlaceholder(Path file, String label, Color color) throws IOException {
        BufferedImage canvas = new BufferedImage(720, 540, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(color);
            graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            graphics.setColor(new Color(255, 250, 242));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
            int width = graphics.getFontMetrics().stringWidth(label);
            graphics.drawString(label, Math.max(30, (canvas.getWidth() - width) / 2), canvas.getHeight() / 2);
        } finally {
            graphics.dispose();
        }
        if (!ImageIO.write(canvas, "png", file.toFile())) throw new IOException("PNG writer is unavailable");
    }
}
