package com.nova.common.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

/**
 * Exposes a {@link CorsConfigurationSource} bean named {@code corsConfigurationSource},
 * which Spring Security's {@code .cors()} picks up automatically.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private final CorsProperties properties;

    public CorsConfig(CorsProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = properties.allowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            configuration.setAllowedOrigins(origins);
        } else {
            configuration.setAllowedOriginPatterns(List.of("*"));
        }
        configuration.setAllowedMethods(
                properties.allowedMethods() != null ? properties.allowedMethods()
                        : List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                properties.allowedHeaders() != null ? properties.allowedHeaders()
                        : List.of("*"));
        configuration.setAllowCredentials(
                properties.allowCredentials() != null && properties.allowCredentials());
        configuration.setMaxAge(3600L);

        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                return configuration;
            }
        };
    }
}
