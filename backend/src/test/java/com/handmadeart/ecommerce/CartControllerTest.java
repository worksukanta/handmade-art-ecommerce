package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.CartController;
import com.handmadeart.ecommerce.dto.cart.AddCartItemRequest;
import com.handmadeart.ecommerce.dto.cart.CartItemResponse;
import com.handmadeart.ecommerce.dto.cart.CartResponse;
import com.handmadeart.ecommerce.dto.cart.UpdateCartItemRequest;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.InsufficientStockException;
import com.handmadeart.ecommerce.exception.ProductNotPurchasableException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import com.handmadeart.ecommerce.service.AuthService;
import com.handmadeart.ecommerce.service.CartService;
import com.handmadeart.ecommerce.service.CatalogueService;
import com.handmadeart.ecommerce.service.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc-based controller tests for cart endpoints.
 *
 * Uses the real SecurityConfig + JwtAuthenticationFilter to verify
 * authentication/authorization rules. CartService is mocked.
 *
 * Test IDs follow the approved Test Strategy (TC-022..TC-031 scope).
 *
 * Covered:
 *   CART-C-01  Unauthenticated request → GET /cart → 401
 *   CART-C-02  Unauthenticated request → POST /cart/items → 401
 *   CART-C-03  CUSTOMER JWT → GET /cart → 200 + CartResponse
 *   CART-C-04  CUSTOMER JWT → GET /cart → empty cart → 200 with empty items
 *   CART-C-05  CUSTOMER JWT → POST /cart/items → 200 + CartResponse
 *   CART-C-06  CUSTOMER JWT → POST /cart/items → product not found → 404
 *   CART-C-07  CUSTOMER JWT → POST /cart/items → PORTFOLIO_ONLY → 409
 *   CART-C-08  CUSTOMER JWT → POST /cart/items → insufficient stock → 409
 *   CART-C-09  CUSTOMER JWT → POST /cart/items → invalid quantity (0) → 400
 *   CART-C-10  CUSTOMER JWT → POST /cart/items → missing productId → 400
 *   CART-C-11  CUSTOMER JWT → PUT /cart/items/{itemId} → 200 + CartResponse
 *   CART-C-12  CUSTOMER JWT → PUT /cart/items/{itemId} → item not found → 404
 *   CART-C-13  CUSTOMER JWT → PUT /cart/items/{itemId} → insufficient stock → 409
 *   CART-C-14  CUSTOMER JWT → PUT /cart/items/{itemId} → invalid quantity (0) → 400
 *   CART-C-15  CUSTOMER JWT → DELETE /cart/items/{itemId} → 200 + CartResponse
 *   CART-C-16  CUSTOMER JWT → DELETE /cart/items/{itemId} → item not found → 404
 *   CART-C-17  CUSTOMER JWT → DELETE /cart/items → clear cart → 204
 *   CART-C-18  Unauthenticated DELETE /cart/items → 401 (second 401 variant)
 *   CART-C-19  CUSTOMER JWT → POST /cart/items → CUSTOM_AVAILABLE product → 409 (FR-CART-01)
 *   CART-C-20  ADMIN JWT → GET /cart → 403 (REST API Spec §8: cart is CUSTOMER-only)
 */
@WebMvcTest(CartController.class)
@Import({
        CartControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CatalogueService catalogueService;

    @MockitoBean
    private AdminCatalogueService adminCatalogueService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public JwtService jwtService(
                @Value("${app.jwt.secret}") String secret,
                @Value("${app.jwt.expiration-ms}") long expMs) {
            return new JwtService(secret, expMs);
        }
    }

    // -------------------------------------------------------------------------
    // Token and mock helpers
    // -------------------------------------------------------------------------

    private String customerToken() {
        return "Bearer " + jwtService.generateToken("customer@example.com", "CUSTOMER");
    }

    private String adminToken() {
        return "Bearer " + jwtService.generateToken("admin@example.com", "ADMIN");
    }

    private UserDetails customerDetails() {
        return User.builder().username("customer@example.com")
                .password("{noop}x").roles("CUSTOMER").build();
    }

    private UserDetails adminDetails() {
        return User.builder().username("admin@example.com")
                .password("{noop}x").roles("ADMIN").build();
    }

    private AppUser buildCustomerEntity() {
        AppUser user = new AppUser();
        user.setEmail("customer@example.com");
        user.setFullName("Test Customer");
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    private CartResponse buildCartResponse(Long cartId, BigDecimal total, List<CartItemResponse> items) {
        // Use reflection-free construction via static factory not available on plain DTO
        // Build via the factory
        return buildCartResponseDirect(cartId, total, items);
    }

    /** Build a CartResponse directly without a Cart entity for test purposes. */
    private CartResponse buildCartResponseDirect(Long cartId, BigDecimal total,
                                                  List<CartItemResponse> items) {
        // CartResponse has no setters but we can set through reflection if needed.
        // The cleanest approach is to use a real Cart + CartItem setup, but for
        // controller tests we just need a mocked service response.
        // Delegate to the static factory in CartResponse using a transient Cart.
        com.handmadeart.ecommerce.entity.Cart cart =
                new com.handmadeart.ecommerce.entity.Cart();
        cart.setUser(buildCustomerEntity());

        // Build a dummy CartResponse that the controller will serialize
        // We use a nested approach: cart has no items in entity; items list is separate.
        // CartResponse.from(cart, items) computes totals from CartItemResponse — but items
        // here are already built. Let's inject directly.
        //
        // Since CartResponse only has getters, we construct it via static factory
        // with empty cart entity and empty item list first, then return.
        // For controller tests, we only need the mock to return a valid response object.
        // We construct via reflection-free approach: build a minimal factory result.
        com.handmadeart.ecommerce.entity.Cart cartWithId =
                new com.handmadeart.ecommerce.entity.Cart();
        cartWithId.setUser(buildCustomerEntity());
        // CartResponse.from does not need real CartItem entities for the items list
        // (those come from CartItemResponse.from). Since we already have CartItemResponse
        // objects, we cannot pass them to CartResponse.from without entities.
        // Instead return a real CartResponse using the actual factory with no items,
        // for minimal verification of controller behavior.
        return CartResponse.from(cartWithId, List.of());
    }

    private CartResponse buildEmptyCartResponse() {
        com.handmadeart.ecommerce.entity.Cart cart =
                new com.handmadeart.ecommerce.entity.Cart();
        cart.setUser(buildCustomerEntity());
        return CartResponse.from(cart, List.of());
    }

    // -------------------------------------------------------------------------
    // CART-C-01: Unauthenticated → GET /cart → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-01: Unauthenticated request to GET /cart returns 401")
    void unauthenticated_getCart_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // -------------------------------------------------------------------------
    // CART-C-02: Unauthenticated → POST /cart/items → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-02: Unauthenticated request to POST /cart/items returns 401")
    void unauthenticated_addItem_returns401() throws Exception {
        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(1);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // CART-C-03: CUSTOMER → GET /cart → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-03: Authenticated CUSTOMER GET /cart returns 200 + CartResponse")
    void customerToken_getCart_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.getCart(any()))
                .thenReturn(buildEmptyCartResponse());

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").exists());
    }

    // -------------------------------------------------------------------------
    // CART-C-04: CUSTOMER → GET /cart → empty cart → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-04: CUSTOMER GET /cart with no items returns 200 with empty items list")
    void customerToken_getCart_emptyCart_returns200WithEmptyList() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.getCart(any()))
                .thenReturn(buildEmptyCartResponse());

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    // -------------------------------------------------------------------------
    // CART-C-05: CUSTOMER → POST /cart/items → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-05: CUSTOMER POST /cart/items with valid product returns 200 + CartResponse")
    void customerToken_addItem_valid_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.addItem(any(), any()))
                .thenReturn(buildEmptyCartResponse());

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    // -------------------------------------------------------------------------
    // CART-C-06: CUSTOMER → POST /cart/items → product not found → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-06: CUSTOMER POST /cart/items with non-existent product returns 404")
    void customerToken_addItem_productNotFound_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.addItem(any(), any()))
                .thenThrow(new ResourceNotFoundException("Product not found: 999"));

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(999L);
        req.setQuantity(1);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // CART-C-07: CUSTOMER → POST /cart/items → PORTFOLIO_ONLY → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-07: CUSTOMER POST /cart/items with PORTFOLIO_ONLY product returns 409")
    void customerToken_addItem_portfolioOnly_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.addItem(any(), any()))
                .thenThrow(new ProductNotPurchasableException(
                        "Portfolio-only products cannot be added to cart"));

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(5L);
        req.setQuantity(1);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_PURCHASABLE"));
    }

    // -------------------------------------------------------------------------
    // CART-C-08: CUSTOMER → POST /cart/items → insufficient stock → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-08: CUSTOMER POST /cart/items with quantity exceeding stock returns 409")
    void customerToken_addItem_insufficientStock_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.addItem(any(), any()))
                .thenThrow(new InsufficientStockException(
                        "Requested quantity 10 exceeds available stock (3)"));

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(10);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
    }

    // -------------------------------------------------------------------------
    // CART-C-09: CUSTOMER → POST /cart/items → invalid quantity (0) → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-09: CUSTOMER POST /cart/items with quantity 0 returns 400")
    void customerToken_addItem_zeroQuantity_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(1L);
        req.setQuantity(0);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -------------------------------------------------------------------------
    // CART-C-10: CUSTOMER → POST /cart/items → missing productId → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-10: CUSTOMER POST /cart/items with missing productId returns 400")
    void customerToken_addItem_missingProductId_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());

        AddCartItemRequest req = new AddCartItemRequest();
        // productId not set
        req.setQuantity(1);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -------------------------------------------------------------------------
    // CART-C-11: CUSTOMER → PUT /cart/items/{itemId} → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-11: CUSTOMER PUT /cart/items/{itemId} with valid quantity returns 200")
    void customerToken_updateItem_valid_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.updateItemQuantity(any(), anyLong(), any()))
                .thenReturn(buildEmptyCartResponse());

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(3);

        mockMvc.perform(put("/api/v1/cart/items/1")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    // -------------------------------------------------------------------------
    // CART-C-12: CUSTOMER → PUT /cart/items/{itemId} → item not found → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-12: CUSTOMER PUT /cart/items/{itemId} with non-existent item returns 404")
    void customerToken_updateItem_notFound_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.updateItemQuantity(any(), anyLong(), any()))
                .thenThrow(new ResourceNotFoundException("Cart item not found"));

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(2);

        mockMvc.perform(put("/api/v1/cart/items/999")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // CART-C-13: CUSTOMER → PUT /cart/items/{itemId} → insufficient stock → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-13: CUSTOMER PUT /cart/items/{itemId} with quantity exceeding stock returns 409")
    void customerToken_updateItem_insufficientStock_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.updateItemQuantity(any(), anyLong(), any()))
                .thenThrow(new InsufficientStockException("Requested quantity 20 exceeds available stock (5)"));

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(20);

        mockMvc.perform(put("/api/v1/cart/items/1")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
    }

    // -------------------------------------------------------------------------
    // CART-C-14: CUSTOMER → PUT /cart/items/{itemId} → invalid quantity (0) → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-14: CUSTOMER PUT /cart/items/{itemId} with quantity 0 returns 400")
    void customerToken_updateItem_zeroQuantity_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(0);

        mockMvc.perform(put("/api/v1/cart/items/1")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -------------------------------------------------------------------------
    // CART-C-15: CUSTOMER → DELETE /cart/items/{itemId} → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-15: CUSTOMER DELETE /cart/items/{itemId} returns 200 + updated cart")
    void customerToken_removeItem_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.removeItem(any(), anyLong()))
                .thenReturn(buildEmptyCartResponse());

        mockMvc.perform(delete("/api/v1/cart/items/1")
                        .header("Authorization", customerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    // -------------------------------------------------------------------------
    // CART-C-16: CUSTOMER → DELETE /cart/items/{itemId} → item not found → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-16: CUSTOMER DELETE /cart/items/{itemId} with non-existent item returns 404")
    void customerToken_removeItem_notFound_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.removeItem(any(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Cart item not found"));

        mockMvc.perform(delete("/api/v1/cart/items/999")
                        .header("Authorization", customerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // CART-C-17: CUSTOMER → DELETE /cart/items → 204
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-17: CUSTOMER DELETE /cart/items clears cart and returns 204")
    void customerToken_clearCart_returns204() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        // clearCart is a void method — default Mockito behavior is fine
        doNothing().when(cartService).clearCart(any());

        mockMvc.perform(delete("/api/v1/cart/items")
                        .header("Authorization", customerToken()))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // CART-C-18: Unauthenticated DELETE /cart/items → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-18: Unauthenticated DELETE /cart/items returns 401")
    void unauthenticated_clearCart_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/cart/items"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // CART-C-20: ADMIN JWT → GET /cart → 403 (CUSTOMER role required)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-20: ADMIN JWT on GET /cart returns 403 (cart is CUSTOMER role only per REST API Spec §8)")
    void adminToken_getCart_returns403() throws Exception {
        // REST API Spec §8 specifies "Authorized roles: CUSTOMER" for all cart endpoints.
        // An ADMIN principal must be rejected with 403, not allowed to operate the cart.
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(adminDetails());

        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", adminToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // -------------------------------------------------------------------------
    // CART-C-19: CUSTOM_AVAILABLE product → 409 (FR-CART-01 enforcement)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CART-C-19: CUSTOMER POST /cart/items with CUSTOM_AVAILABLE product returns 409 (FR-CART-01)")
    void customerToken_addItem_customAvailable_returns409() throws Exception {
        // FR-CART-01: only READY_MADE products may be added to cart.
        // CUSTOM_AVAILABLE follows the commissioned custom-artwork workflow, not cart/checkout.
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());
        when(currentUserService.getAuthenticatedUser())
                .thenReturn(buildCustomerEntity());
        when(cartService.addItem(any(), any()))
                .thenThrow(new ProductNotPurchasableException(
                        "Only ready-made products can be added to cart"));

        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(2L);
        req.setQuantity(1);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", customerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_PURCHASABLE"));
    }
}
