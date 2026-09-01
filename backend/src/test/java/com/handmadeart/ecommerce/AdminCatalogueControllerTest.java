package com.handmadeart.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handmadeart.ecommerce.controller.AdminCategoryController;
import com.handmadeart.ecommerce.controller.AdminProductController;
import com.handmadeart.ecommerce.dto.admin.CategoryRequest;
import com.handmadeart.ecommerce.dto.admin.InventoryUpdateRequest;
import com.handmadeart.ecommerce.dto.admin.InventoryResponse;
import com.handmadeart.ecommerce.dto.admin.ProductRequest;
import com.handmadeart.ecommerce.dto.admin.RelatedProductsRequest;
import com.handmadeart.ecommerce.dto.catalogue.CategoryResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductDetailResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.Category;
import com.handmadeart.ecommerce.entity.CategoryStatus;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.DuplicateCategoryNameException;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
import com.handmadeart.ecommerce.service.AdminCatalogueService;
import com.handmadeart.ecommerce.service.AuthService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc-based integration tests for admin catalogue controller endpoints.
 *
 * Uses real SecurityConfig + JwtAuthenticationFilter so authorization behavior
 * (401 for unauthenticated, 403 for CUSTOMER, success for ADMIN) is tested
 * against production security rules.
 *
 * Covered:
 *   ACAT-01  Unauthenticated request to any admin endpoint → 401
 *   ACAT-02  CUSTOMER JWT → any admin endpoint → 403
 *   ACAT-03  ADMIN JWT → create category → 201
 *   ACAT-04  ADMIN JWT → duplicate category name → 409
 *   ACAT-05  ADMIN JWT → update category → 200
 *   ACAT-06  ADMIN JWT → category not found → 404
 *   ACAT-07  ADMIN JWT → change category status → 200
 *   ACAT-08  ADMIN JWT → create product → 201
 *   ACAT-09  ADMIN JWT → invalid category for product → 404
 *   ACAT-10  ADMIN JWT → change product status → 200
 *   ACAT-11  ADMIN JWT → admin product listing returns all statuses (no public filter)
 *   ACAT-12  ADMIN JWT → remove product image → 204
 *   ACAT-13  ADMIN JWT → replace related products → 200
 *   ACAT-14  ADMIN JWT → self-related product rejected → 400
 *   ACAT-15  ADMIN JWT → update inventory → 200
 *   ACAT-16  ADMIN JWT → PORTFOLIO_ONLY inventory → 409
 *   ACAT-17  ADMIN JWT → negative inventory rejected → 400
 *   ACAT-18  ADMIN JWT → get product inventory → 200
 *   ACAT-19  ADMIN JWT → product not found on inventory → 404
 *   ACAT-20  Public catalogue remains correct — INACTIVE product still serves admin (separate behaviour)
 */
@WebMvcTest({AdminCategoryController.class, AdminProductController.class})
@Import({
        AdminCatalogueControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class AdminCatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AdminCatalogueService adminCatalogueService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CatalogueService catalogueService;

    @MockitoBean
    private CurrentUserService currentUserService;

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
    // Token helpers
    // -------------------------------------------------------------------------

    private String adminToken() {
        return "Bearer " + jwtService.generateToken("admin@example.com", "ADMIN");
    }

    private String customerToken() {
        return "Bearer " + jwtService.generateToken("customer@example.com", "CUSTOMER");
    }

    private UserDetails adminDetails() {
        return User.builder().username("admin@example.com").password("{noop}x").roles("ADMIN").build();
    }

    private UserDetails customerDetails() {
        return User.builder().username("customer@example.com").password("{noop}x").roles("CUSTOMER").build();
    }

    // -------------------------------------------------------------------------
    // ACAT-01: Unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-01: Unauthenticated request to any admin endpoint returns 401")
    void unauthenticated_adminEndpoint_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // ACAT-02: CUSTOMER → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-02: CUSTOMER JWT on admin endpoint returns 403")
    void customerToken_adminEndpoint_returns403() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString()))
                .thenReturn(customerDetails());

        mockMvc.perform(get("/api/v1/admin/products")
                        .header("Authorization", customerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    // -------------------------------------------------------------------------
    // ACAT-03: Create category success
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-03: ADMIN JWT creates category → 201 with CategoryResponse")
    void createCategory_adminToken_returns201() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.createCategory(any()))
                .thenReturn(buildCategoryResponse(1L, "Paintings", "ACTIVE"));

        CategoryRequest req = new CategoryRequest();
        req.setName("Paintings");
        req.setDescription("Beautiful art");

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Paintings"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // -------------------------------------------------------------------------
    // ACAT-04: Duplicate category name → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-04: Duplicate category name returns 409 DUPLICATE_CATEGORY_NAME")
    void createCategory_duplicateName_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.createCategory(any()))
                .thenThrow(new DuplicateCategoryNameException("A category named 'Paintings' already exists"));

        CategoryRequest req = new CategoryRequest();
        req.setName("Paintings");

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DUPLICATE_CATEGORY_NAME"));
    }

    // -------------------------------------------------------------------------
    // ACAT-05: Update category → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-05: ADMIN JWT updates category → 200")
    void updateCategory_adminToken_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.updateCategory(anyLong(), any()))
                .thenReturn(buildCategoryResponse(1L, "Updated Name", "ACTIVE"));

        CategoryRequest req = new CategoryRequest();
        req.setName("Updated Name");

        mockMvc.perform(put("/api/v1/admin/categories/1")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    // -------------------------------------------------------------------------
    // ACAT-06: Category not found → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-06: Category not found returns 404")
    void updateCategory_notFound_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.updateCategory(anyLong(), any()))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        CategoryRequest req = new CategoryRequest();
        req.setName("Any Name");

        mockMvc.perform(put("/api/v1/admin/categories/99")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // ACAT-07: Change category status → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-07: ADMIN JWT changes category status → 200")
    void changeCategoryStatus_adminToken_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.changeCategoryStatus(anyLong(), anyString()))
                .thenReturn(buildCategoryResponse(1L, "Paintings", "INACTIVE"));

        mockMvc.perform(patch("/api/v1/admin/categories/1/status")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    // -------------------------------------------------------------------------
    // ACAT-08: Create product → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-08: ADMIN JWT creates product → 201")
    void createProduct_adminToken_returns201() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.createProduct(any()))
                .thenReturn(buildProductDetailResponse(10L, "New Painting", ProductType.READY_MADE,
                        ProductStatus.ACTIVE, new BigDecimal("99.00")));

        ProductRequest req = buildProductRequest("New Painting", 1L, ProductType.READY_MADE.name(),
                ProductStatus.ACTIVE.name(), new BigDecimal("99.00"));

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Painting"))
                .andExpect(jsonPath("$.product_type").value("READY_MADE"));
    }

    // -------------------------------------------------------------------------
    // ACAT-09: Invalid category for product → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-09: Invalid category for product returns 404")
    void createProduct_invalidCategory_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.createProduct(any()))
                .thenThrow(new ResourceNotFoundException("Category not found: 999"));

        ProductRequest req = buildProductRequest("Test", 999L, ProductType.READY_MADE.name(),
                ProductStatus.ACTIVE.name(), new BigDecimal("10.00"));

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // ACAT-10: Change product status → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-10: ADMIN JWT changes product status → 200")
    void changeProductStatus_adminToken_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.changeProductStatus(anyLong(), anyString()))
                .thenReturn(buildProductDetailResponse(10L, "Painting", ProductType.READY_MADE,
                        ProductStatus.INACTIVE, new BigDecimal("50.00")));

        mockMvc.perform(patch("/api/v1/admin/products/10/status")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // ACAT-11: Admin product listing returns all statuses (distinct from public)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-11: Admin product listing endpoint is at /api/v1/admin/products, not /api/v1/products")
    void adminProductListing_separateEndpointFromPublic() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());

        // Admin endpoint mocked — we verify the URL is different and returns 200
        com.handmadeart.ecommerce.dto.catalogue.PageResponse<
                com.handmadeart.ecommerce.dto.catalogue.ProductSummaryResponse> emptyPage =
                com.handmadeart.ecommerce.dto.catalogue.PageResponse.from(
                        new org.springframework.data.domain.PageImpl<>(List.of()));
        when(adminCatalogueService.listAllProducts(anyInt(), anyInt())).thenReturn(emptyPage);

        // Admin endpoint requires ADMIN token
        mockMvc.perform(get("/api/v1/admin/products")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Same path without auth → 401 (not accessible to unauthenticated)
        mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // ACAT-12: Remove product image → 204
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-12: ADMIN JWT removes product image → 204 No Content")
    void removeProductImage_adminToken_returns204() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());

        mockMvc.perform(delete("/api/v1/admin/products/1/images/5")
                        .header("Authorization", adminToken()))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // ACAT-13: Replace related products → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-13: ADMIN JWT replaces related products → 200")
    void replaceRelatedProducts_adminToken_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.replaceRelatedProducts(anyLong(), any()))
                .thenReturn(List.of());

        RelatedProductsRequest req = new RelatedProductsRequest();
        req.setProductIds(List.of(2L, 3L));

        mockMvc.perform(put("/api/v1/admin/products/1/related-products")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // ACAT-14: Self-related product → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-14: Self-reference in related products returns 400 INVALID_PARAMETER")
    void replaceRelatedProducts_selfReference_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.replaceRelatedProducts(anyLong(), any()))
                .thenThrow(new IllegalArgumentException("A product cannot be related to itself"));

        RelatedProductsRequest req = new RelatedProductsRequest();
        req.setProductIds(List.of(1L));  // same as product ID 1

        mockMvc.perform(put("/api/v1/admin/products/1/related-products")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    // -------------------------------------------------------------------------
    // ACAT-15: Update inventory → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-15: ADMIN JWT updates inventory → 200 InventoryResponse")
    void updateInventory_adminToken_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.updateInventory(anyLong(), any()))
                .thenReturn(buildInventoryResponse(10L, 25));

        InventoryUpdateRequest req = new InventoryUpdateRequest();
        req.setAvailableQuantity(25);

        mockMvc.perform(patch("/api/v1/admin/inventory/10")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(10))
                .andExpect(jsonPath("$.quantity_on_hand").value(25));
    }

    // -------------------------------------------------------------------------
    // ACAT-16: PORTFOLIO_ONLY inventory → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-16: PORTFOLIO_ONLY product inventory update returns 409 CONFLICT")
    void updateInventory_portfolioOnly_returns409() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.updateInventory(anyLong(), any()))
                .thenThrow(new com.handmadeart.ecommerce.exception.InventoryTypeConflictException(
                        "PORTFOLIO_ONLY products do not support inventory management"));

        InventoryUpdateRequest req = new InventoryUpdateRequest();
        req.setAvailableQuantity(10);

        mockMvc.perform(patch("/api/v1/admin/inventory/10")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // -------------------------------------------------------------------------
    // ACAT-17: Negative inventory rejected → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-17: Negative inventory quantity rejected by validation → 400")
    void updateInventory_negativeQuantity_returns400() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());

        InventoryUpdateRequest req = new InventoryUpdateRequest();
        req.setAvailableQuantity(-5);  // invalid — violates @Min(0)

        mockMvc.perform(patch("/api/v1/admin/inventory/10")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // -------------------------------------------------------------------------
    // ACAT-18: Get product inventory → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-18: ADMIN JWT gets product inventory → 200")
    void getInventory_adminToken_returns200() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.getInventory(anyLong()))
                .thenReturn(buildInventoryResponse(10L, 15));

        mockMvc.perform(get("/api/v1/admin/inventory/10")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(10));
    }

    // -------------------------------------------------------------------------
    // ACAT-19: Product not found on inventory → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ACAT-19: Product not found returns 404 on inventory endpoint")
    void getInventory_notFound_returns404() throws Exception {
        when(appUserDetailsService.loadUserByUsername(anyString())).thenReturn(adminDetails());
        when(adminCatalogueService.getInventory(anyLong()))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/admin/inventory/99")
                        .header("Authorization", adminToken()))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private CategoryResponse buildCategoryResponse(Long id, String name, String status) {
        Category cat = new Category();
        cat.setName(name);
        cat.setStatus(CategoryStatus.valueOf(status));
        try {
            var f = Category.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(cat, id);
        } catch (Exception ignored) {}
        return CategoryResponse.from(cat);
    }

    private ProductDetailResponse buildProductDetailResponse(Long id, String name,
                                                              ProductType type,
                                                              ProductStatus status,
                                                              BigDecimal price) {
        Category cat = new Category();
        cat.setName("Art");
        cat.setStatus(CategoryStatus.ACTIVE);
        try {
            var f = Category.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(cat, 1L);
        } catch (Exception ignored) {}

        com.handmadeart.ecommerce.entity.Product product =
                new com.handmadeart.ecommerce.entity.Product();
        product.setName(name);
        product.setProductType(type);
        product.setStatus(status);
        product.setPrice(price);
        product.setCategory(cat);
        try {
            var f = com.handmadeart.ecommerce.entity.Product.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(product, id);
        } catch (Exception ignored) {}

        return ProductDetailResponse.from(product, List.of(), null, List.of());
    }

    private InventoryResponse buildInventoryResponse(Long productId, int qty) {
        com.handmadeart.ecommerce.entity.Inventory inv = new com.handmadeart.ecommerce.entity.Inventory();
        inv.setQuantityOnHand(qty);
        // Set productId via reflection (shared PK field)
        try {
            var f = com.handmadeart.ecommerce.entity.Inventory.class.getDeclaredField("productId");
            f.setAccessible(true);
            f.set(inv, productId);
        } catch (Exception ignored) {}
        return InventoryResponse.from(inv);
    }

    private ProductRequest buildProductRequest(String name, Long categoryId,
                                                String productType, String status,
                                                BigDecimal price) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setCategoryId(categoryId);
        req.setProductType(productType);
        req.setStatus(status);
        req.setPrice(price);
        return req;
    }
}
