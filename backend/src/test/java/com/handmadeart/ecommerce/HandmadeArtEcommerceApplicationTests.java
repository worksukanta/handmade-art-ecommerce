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
 * CartService requires CartRepository and CartItemRepository — mocked here (Phase 3C.1).
 * CheckoutService requires AddressRepository, CustomerOrderRepository, OrderItemRepository (Phase 3D.1).
 * PaymentService requires PaymentRepository (Phase 3D.2).
 * CustomArtworkRequestService requires CustomOrderRequestRepository, CustomOrderImageRepository (Phase 3E.1).
 * QuotationService requires QuotationRepository, CustomOrderRequestRepository (Phase 3E.1).
 * CustomAdvancePaymentService requires ShipmentRepository (Phase 3E.2).
 * AdminProductionService requires ShipmentRepository, CustomerOrderRepository (Phase 3E.2).
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
        // If the Spring context starts without error, this test passes.
    }

}
