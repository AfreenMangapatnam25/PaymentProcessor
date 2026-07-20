package com.paymentprocessor.merchantservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata and the API-key (bearer) security scheme surfaced in Swagger UI. */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "ApiKeyAuth";

    @Bean
    public OpenAPI merchantServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Merchant Service API")
                        .version("1.0.0")
                        .description("System of record for merchant identity, onboarding, KYB/KYC "
                                + "compliance status, financial configuration, integration "
                                + "credentials, and merchant lifecycle.")
                        .contact(new Contact().name("Payments Platform").email("platform@paymentprocessor.com"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("Present an API key as 'Bearer <keyId>.<secret>' or the "
                                        + "platform admin key.")));
    }
}
