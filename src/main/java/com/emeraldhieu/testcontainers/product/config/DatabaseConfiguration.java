package com.emeraldhieu.testcontainers.product.config;

import com.emeraldhieu.testcontainers.product.logic.ProductRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackageClasses = ProductRepository.class)
@EnableTransactionManagement
public class DatabaseConfiguration {

}
