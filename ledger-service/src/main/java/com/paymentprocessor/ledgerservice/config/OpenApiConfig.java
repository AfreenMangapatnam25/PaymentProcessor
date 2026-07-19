package com.paymentprocessor.ledgerservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Ledger Service API")
                .version("1.0.0")
                .description("Immutable, double-entry financial book of record for the payment platform. "
                        + "All monetary amounts are expressed in minor currency units (e.g. cents).")
                .contact(new Contact().name("Payments Platform").email("platform@paymentprocessor.com"))
                .license(new License().name("Proprietary")));
    }
}
