package com.nova.finance.budget;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Budget module's configurable {@link BudgetProperties} into the context,
 * mirroring how {@code CorsConfig}/{@code FlywayConfig} enable their own properties.
 */
@Configuration
@EnableConfigurationProperties(BudgetProperties.class)
public class BudgetConfig {
}
