package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.controller.CategoryController;
import com.handmadeart.ecommerce.controller.ProductController;
import com.handmadeart.ecommerce.dto.catalogue.CategoryResponse;
import com.handmadeart.ecommerce.dto.catalogue.PageResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductDetailResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductImageResponse;
import com.handmadeart.ecommerce.dto.catalogue.ProductSummaryResponse;
import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.Category;
import com.handmadeart.ecommerce.entity.CategoryStatus;
import com.handmadeart.ecommerce.entity.Inventory;
import com.handmadeart.ecommerce.entity.Product;
import com.handmadeart.ecommerce.entity.ProductImage;
import com.handmadeart.ecommerce.entity.ProductStatus;
import com.handmadeart.ecommerce.entity.ProductType;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.AppUserRepository;
import com.handmadeart.ecommerce.security.AppUserDetailsService;
import com.handmadeart.ecommerce.security.JwtService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for the public catalogue endpoints.
 *
 * Uses @WebMvcTest with the real SecurityConfig imported — verifies that the
 * catalogue endpoints are accessible without a JWT (anonymous access).
 *
 * CatalogueService is mocked at the boundary; the tests verify:
 * - HTTP status codes
 * - JSON response shapes
 * - Anonymous access (no Authorization header required)
 * - 404 error envelope for not-found/inactive resources
 * - 400 error envelope for invalid sort field
 * - DTO does not expose internal fields
 *
 * Covered:
 *   CAT-01  GET /categories returns 200 + array without auth
 *   CAT-02  GET /categories/{id} returns 200 + CategoryResponse without auth
 *   CAT-03  GET /categories/{id} returns 404 for inactive/missing category
 *   PROD-01 GET /products returns 200 + PageResponse without auth
 *   PROD-02 GET /products/{id} returns 200 + ProductDetailResponse without auth
 *   PROD-03 GET /products/{id} returns 404 for inactive/missing product
 *   PROD-04 GET /products/{id}/related-products returns 200 + array without auth
 *   PROD-05 GET /products?sort=INVALID returns 400 with structured error
 *   PROD-06 ProductDetailResponse does not expose password_hash or status internals
 */
@WebMvcTest({CategoryController.class, ProductController.class})
@Import({
        CatalogueControllerTest.TestSecurityConfig.class,
        com.handmadeart.ecommerce.config.SecurityConfig.class,
        com.handmadeart.ecommerce.security.JwtAuthenticationFilter.class,
        com.handmadeart.ecommerce.security.AuthEntryPoint.class,
        com.handmadeart.ecommerce.security.ApiAccessDeniedHandler.class,
        com.handmadeart.ecommerce.exception.GlobalExceptionHandler.class
})
class CatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogueService catalogueService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    // -------------------------------------------------------------------------
    // Test configuration — provides JwtService bean
    // -------------------------------------------------------------------------

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
    // CAT-01: GET /categories — anonymous, returns array
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAT-01: GET /api/v1/categories returns 200 without authentication")
    void listCategories_anonymousAccess_returns200() throws Exception {
        CategoryResponse cat = buildCategoryResponse(1L, "Paintings");
        when(catalogueService.listActiveCategories()).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Paintings"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    // -------------------------------------------------------------------------
    // CAT-02: GET /categories/{id} — anonymous, returns single item
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAT-02: GET /api/v1/categories/{id} returns 200 + CategoryResponse without auth")
    void getCategory_anonymousAccess_returns200() throws Exception {
        when(catalogueService.getActiveCategory(1L))
                .thenReturn(buildCategoryResponse(1L, "Sculptures"));

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sculptures"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // -------------------------------------------------------------------------
    // CAT-03: GET /categories/{id} — 404 for inactive/missing
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CAT-03: GET /api/v1/categories/{id} returns 404 for inactive/missing category")
    void getCategory_inactiveOrMissing_returns404() throws Exception {
        when(catalogueService.getActiveCategory(99L))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(get("/api/v1/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // PROD-01: GET /products — anonymous, returns PageResponse
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PROD-01: GET /api/v1/products returns 200 + PageResponse without authentication")
    void listProducts_anonymousAccess_returns200WithPage() throws Exception {
        ProductSummaryResponse summary = buildProductSummary(10L, "Sunflower Print");
        PageResponse<ProductSummaryResponse> page = buildPage(List.of(summary), 0, 20, 1, 1);

        when(catalogueService.listProducts(any(), any(), any(), any(), any(), any(),
                anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].name").value("Sunflower Print"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.total_pages").value(1));
    }

    // -------------------------------------------------------------------------
    // PROD-02: GET /products/{id} — anonymous, returns detail
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PROD-02: GET /api/v1/products/{id} returns 200 + ProductDetailResponse without auth")
    void getProduct_anonymousAccess_returns200() throws Exception {
        ProductDetailResponse detail = buildProductDetail(10L, "Sunflower Print");
        when(catalogueService.getProductDetail(10L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/products/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Sunflower Print"))
                .andExpect(jsonPath("$.availability").exists())
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.related_products").isArray());
    }

    // -------------------------------------------------------------------------
    // PROD-03: GET /products/{id} — 404 for inactive/missing
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PROD-03: GET /api/v1/products/{id} returns 404 for inactive/missing product")
    void getProduct_inactiveOrMissing_returns404() throws Exception {
        when(catalogueService.getProductDetail(99L))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/products/99"));
    }

    // -------------------------------------------------------------------------
    // PROD-04: GET /products/{id}/related-products — anonymous, returns array
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PROD-04: GET /api/v1/products/{id}/related-products returns 200 + array without auth")
    void getRelatedProducts_anonymousAccess_returns200() throws Exception {
        ProductSummaryResponse related = buildProductSummary(11L, "Mountain Watercolour");
        when(catalogueService.getRelatedProducts(10L)).thenReturn(List.of(related));

        mockMvc.perform(get("/api/v1/products/10/related-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].name").value("Mountain Watercolour"));
    }

    // -------------------------------------------------------------------------
    // PROD-05: GET /products?sort=INVALID returns 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PROD-05: GET /api/v1/products?sort=INVALID returns 400 with structured error")
    void listProducts_invalidSort_returns400() throws Exception {
        when(catalogueService.listProducts(any(), any(), any(), any(),
                any(), any(), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException(
                        "Invalid sort field 'INVALID'. Allowed: name, price, created_at"));

        mockMvc.perform(get("/api/v1/products?sort=INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    // -------------------------------------------------------------------------
    // PROD-06: ProductDetailResponse does not expose internal fields
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PROD-06: ProductDetailResponse does not contain password_hash or raw status fields")
    void getProduct_responseDoesNotExposeInternalFields() throws Exception {
        ProductDetailResponse detail = buildProductDetail(10L, "Painting");
        when(catalogueService.getProductDetail(10L)).thenReturn(detail);

        String responseBody = mockMvc.perform(get("/api/v1/products/10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Internal fields must not appear in the response
        org.assertj.core.api.Assertions.assertThat(responseBody)
                .doesNotContain("password_hash")
                .doesNotContain("passwordHash")
                .doesNotContain("INACTIVE");   // status is not in ProductDetailResponse
    }

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private CategoryResponse buildCategoryResponse(Long id, String name) {
        Category c = new Category();
        c.setName(name);
        c.setStatus(CategoryStatus.ACTIVE);
        try {
            var f = Category.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception ignored) {}
        return CategoryResponse.from(c);
    }

    private ProductSummaryResponse buildProductSummary(Long id, String name) {
        Category cat = new Category();
        cat.setName("Test Category");
        cat.setStatus(CategoryStatus.ACTIVE);
        try {
            var cf = Category.class.getDeclaredField("id");
            cf.setAccessible(true);
            cf.set(cat, 1L);
        } catch (Exception ignored) {}

        Product p = new Product();
        p.setName(name);
        p.setStatus(ProductStatus.ACTIVE);
        p.setProductType(ProductType.READY_MADE);
        p.setCategory(cat);
        p.setPrice(BigDecimal.valueOf(150.00));
        try {
            var f = Product.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {}
        return ProductSummaryResponse.from(p, Collections.emptyList());
    }

    private ProductDetailResponse buildProductDetail(Long id, String name) {
        Category cat = new Category();
        cat.setName("Test Category");
        cat.setStatus(CategoryStatus.ACTIVE);
        try {
            var cf = Category.class.getDeclaredField("id");
            cf.setAccessible(true);
            cf.set(cat, 1L);
        } catch (Exception ignored) {}

        Product p = new Product();
        p.setName(name);
        p.setStatus(ProductStatus.ACTIVE);
        p.setProductType(ProductType.READY_MADE);
        p.setCategory(cat);
        p.setPrice(BigDecimal.valueOf(150.00));
        p.setDescription("A beautiful piece.");
        try {
            var f = Product.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {}

        Inventory inv = new Inventory();
        inv.setQuantityOnHand(3);

        return ProductDetailResponse.from(p, Collections.emptyList(), inv,
                Collections.emptyList());
    }

    private <T> PageResponse<T> buildPage(List<T> content, int page, int size,
                                          long totalElements, int totalPages) {
        // Build a PageResponse manually (no Spring Page dependency in test helpers)
        org.springframework.data.domain.PageImpl<T> springPage =
                new org.springframework.data.domain.PageImpl<>(
                        content,
                        org.springframework.data.domain.PageRequest.of(page, size),
                        totalElements);
        return PageResponse.from(springPage);
    }
}
