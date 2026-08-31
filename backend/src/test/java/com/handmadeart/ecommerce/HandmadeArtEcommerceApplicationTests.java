package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies that the Spring application context loads successfully
 * without requiring a live database connection.
 *
 * AppUserRepository is mocked because the test application.yml excludes
 * DataSource and JPA auto-configuration (no PostgreSQL needed for this test),
 * but SecurityConfig → AppUserDetailsService depends on AppUserRepository.
 */
@SpringBootTest
class HandmadeArtEcommerceApplicationTests {

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    void contextLoads() {
        // If the Spring context starts without error, this test passes.
    }

}
