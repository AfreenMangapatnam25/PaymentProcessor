package com.paymentprocessor.authorization.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authorizationServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Authorization Service API")
                .version("1.0.0")
                .description("Payment authorization (authorize, capture, reversal, re-authorization) "
                        + "and RBAC/ABAC access-control decisioning for the payment platform.")
                .contact(new Contact().name("Payments Platform").email("payments@paymentprocessor.com"))
                .license(new License().name("Proprietary")));
    }
}
