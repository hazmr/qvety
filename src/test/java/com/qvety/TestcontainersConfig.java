package com.qvety;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One Postgres container per test JVM, shared by every integration test.
 * @ServiceConnection wires spring.datasource.* from the container automatically.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18")
            .withDatabaseName("qvety")
            .withUsername("qvety_owner")
            .withPassword("qvety_owner");
    }
}
