package com.handmadeart.ecommerce;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database infrastructure integration test.
 *
 * This test class verifies:
 *   1. Spring Boot creates the DataSource successfully.
 *   2. A live JDBC connection to PostgreSQL can be established.
 *   3. Flyway runs and creates its schema_history table.
 *
 * ACTIVATION:
 *   This test requires a running PostgreSQL instance.
 *   It is excluded from the default Maven test run (tag = "db-integration").
 *   Run explicitly with:
 *
 *     mvn clean test -Dgroups=db-integration -Dspring.profiles.active=db-integration
 *
 *   Environment variables required:
 *     DB_URL       = jdbc:postgresql://localhost:5432/handmade_art_ecommerce_test
 *     DB_USERNAME  = <test db user>
 *     DB_PASSWORD  = <test db password>
 *
 * This test does NOT test domain repositories because no domain entities exist yet.
 * Domain-level persistence tests will be added in Phase 2B and beyond.
 */
@Tag("db-integration")
@SpringBootTest
@ActiveProfiles("db-integration")
class DatabaseInfrastructureIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void dataSourceIsConfigured() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    void postgresConnectionCanBeEstablished() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(5)).isTrue();
        }
    }

    @Test
    void flywaySchemaHistoryTableExists() throws Exception {
        try (Connection connection = dataSource.getConnection();
             ResultSet rs = connection.getMetaData().getTables(
                     null, null, "flyway_schema_history", new String[]{"TABLE"})) {
            assertThat(rs.next())
                    .as("flyway_schema_history table should exist after Flyway initialization")
                    .isTrue();
        }
    }

    @Test
    void flywayBaselineMigrationWasApplied() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT version, description, success " +
                     "FROM flyway_schema_history " +
                     "WHERE version = '1'")) {
            assertThat(rs.next())
                    .as("V1 baseline migration should be recorded in flyway_schema_history")
                    .isTrue();
            assertThat(rs.getBoolean("success"))
                    .as("V1 baseline migration should have succeeded")
                    .isTrue();
        }
    }

}
