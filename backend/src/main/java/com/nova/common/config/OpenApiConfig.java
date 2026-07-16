package com.nova.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger documentation configuration. Authentication is declared as a
 * placeholder bearer scheme so later phases can wire it without restructuring docs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI novaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nova API")
                        .description("AI-powered personal finance platform API.")
                        .version("v1")
                        .contact(new Contact().name("Nova Engineering").url("https://nova.local"))
                        .license(new License().name("Proprietary")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
