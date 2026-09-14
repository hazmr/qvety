package com.qvety;

import org.springframework.boot.flyway.autoconfigure.FlywayConnectionDetails;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One Postgres container per test JVM, shared by every integration test.
 * Flyway connects as the owner (container superuser); the application as qvety_app, which V3 creates.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18")
            .withDatabaseName("qvety")
            .withUsername("qvety_owner")
            .withPassword("qvety_owner");
    }

    @Bean
    JdbcConnectionDetails appConnection(PostgreSQLContainer postgres) {
        return new JdbcConnectionDetails() {
            @Override public String getJdbcUrl() { return postgres.getJdbcUrl(); }
            @Override public String getUsername() { return "qvety_app"; }
            @Override public String getPassword() { return "qvety_app"; }
        };
    }

    @Bean
    FlywayConnectionDetails flywayConnection(PostgreSQLContainer postgres) {
        return new FlywayConnectionDetails() {
            @Override public String getJdbcUrl() { return postgres.getJdbcUrl(); }
            @Override public String getUsername() { return postgres.getUsername(); }
            @Override public String getPassword() { return postgres.getPassword(); }
        };
    }
}
