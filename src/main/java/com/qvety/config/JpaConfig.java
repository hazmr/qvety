package com.qvety.config;

import com.qvety.tenant.TenantTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class JpaConfig {

    /** Replaces Boot's default JpaTransactionManager so every transaction carries the tenant. */
    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new TenantTransactionManager(emf);
    }
}
