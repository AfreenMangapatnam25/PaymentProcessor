package com.paymentprocessor.limit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI limitServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Limit Service API")
                .version("1.0.0")
                .description("""
                        Enforces spending and transaction limits before a payment is authorized.
                        Supports per-transaction, daily, weekly and monthly amount/count limits with
                        a reserve → commit → release lifecycle that prevents race conditions and
                        double spending.""")
                .contact(new Contact().name("Payments Platform").email("payments@paymentprocessor.com"))
                .license(new License().name("Proprietary")));
    }
}
