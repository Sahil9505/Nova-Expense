package com.nova.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS settings sourced from environment variables so deployments can open specific
 * origins without rebuilding. Binds to the {@code nova.cors.*} namespace.
 */
@ConfigurationProperties(prefix = "nova.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        Boolean allowCredentials
) {
}
