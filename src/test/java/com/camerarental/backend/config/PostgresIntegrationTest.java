package com.camerarental.backend.config;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.*;

/**
 * Base configuration for integration tests using PostgreSQL via TestContainers.
 * 
 * Tests extending {@link AbstractPostgresIT} will automatically:
 * - Start a PostgreSQL container
 * - Configure Spring datasource to use it
 * - Use the "test-pg" profile
 * 
 * The container is shared across all test classes for performance (singleton pattern).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test-pg")
@Testcontainers
public @interface PostgresIntegrationTest {
}
