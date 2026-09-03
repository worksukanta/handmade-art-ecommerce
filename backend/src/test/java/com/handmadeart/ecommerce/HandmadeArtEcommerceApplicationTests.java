package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.repository.AddressRepository;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import com.handmadeart.ecommerce.repository.CartItemRepository;
import com.handmadeart.ecommerce.repository.CartRepository;
import com.handmadeart.ecommerce.repository.CategoryRepository;
import com.handmadeart.ecommerce.repository.CustomOrderImageRepository;
import com.handmadeart.ecommerce.repository.CustomOrderRequestRepository;
import com.handmadeart.ecommerce.repository.CustomerOrderRepository;
import com.handmadeart.ecommerce.repository.InventoryRepository;
import com.handmadeart.ecommerce.repository.OrderItemRepository;
import com.handmadeart.ecommerce.repository.PaymentRepository;
import com.handmadeart.ecommerce.repository.ProductImageRepository;
import com.handmadeart.ecommerce.repository.ProductRelatedRepository;
import com.handmadeart.ecommerce.repository.ProductRepository;
import com.handmadeart.ecommerce.repository.QuotationRepository;
import com.handmadeart.ecommerce.repository.ShipmentRepository;
import com.handmadeart.ecommerce.config.DevelopmentDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Spring application context loads successfully
 * without requiring a live database connection.
 *
 * Repositories are mocked because the test application.yml excludes
 * DataSource and JPA auto-configuration (no PostgreSQL needed for this test).
 * All repositories transitively required by the full Spring context must be listed here.
 * AdminCatalogueService uses @Value for upload dir — no additional mock needed (uses default).
 * CartService requires CartRepository and CartItemRepository — mocked here (Phase 3C.1).
 * CheckoutService requires AddressRepository, CustomerOrderRepository, OrderItemRepository (Phase 3D.1).
 * PaymentService requires PaymentRepository (Phase 3D.2).
 * CustomArtworkRequestService requires CustomOrderRequestRepository, CustomOrderImageRepository (Phase 3E.1).
 * QuotationService requires QuotationRepository, CustomOrderRequestRepository (Phase 3E.1).
 * CustomAdvancePaymentService requires ShipmentRepository (Phase 3E.2).
 * AdminProductionService requires ShipmentRepository, CustomerOrderRepository (Phase 3E.2).
 */
@SpringBootTest(properties = {
        "app.seed.enabled=false",
        "spring.servlet.multipart.max-file-size=10MB",
        "spring.servlet.multipart.max-request-size=12MB"
})
class HandmadeArtEcommerceApplicationTests {

    private final ApplicationContext applicationContext;
    private final MultipartProperties multipartProperties;

    @Autowired
    HandmadeArtEcommerceApplicationTests(ApplicationContext applicationContext,
                                          MultipartProperties multipartProperties) {
        this.applicationContext = applicationContext;
        this.multipartProperties = multipartProperties;
    }

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

    @MockitoBean
    private CartRepository cartRepository;

    @MockitoBean
    private CartItemRepository cartItemRepository;

    @MockitoBean
    private AddressRepository addressRepository;

    @MockitoBean
    private CustomerOrderRepository customerOrderRepository;

    @MockitoBean
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private CustomOrderRequestRepository customOrderRequestRepository;

    @MockitoBean
    private CustomOrderImageRepository customOrderImageRepository;

    @MockitoBean
    private QuotationRepository quotationRepository;

    @MockitoBean
    private ShipmentRepository shipmentRepository;

    @Test
    void contextLoads() {
        assertThat(applicationContext.getBeansOfType(DevelopmentDataSeeder.class)).isEmpty();
        assertThat(multipartProperties.getMaxFileSize().toBytes()).isEqualTo(10L * 1024 * 1024);
        assertThat(multipartProperties.getMaxRequestSize().toBytes()).isEqualTo(12L * 1024 * 1024);
    }

}
