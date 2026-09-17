package com.UVGgo.backend;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Ejecuta el mismo contrato HTTP y las migraciones en una base PostgreSQL exclusiva de pruebas. */
@Tag("postgres")
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches="jdbc:postgresql:.*")
class PostgresApiTests extends BackendApiTests {
    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",() -> System.getenv("TEST_DB_URL"));
        registry.add("spring.datasource.username",() -> System.getenv("TEST_DB_USER"));
        registry.add("spring.datasource.password",() -> System.getenv("TEST_DB_PASSWORD"));
    }
}
