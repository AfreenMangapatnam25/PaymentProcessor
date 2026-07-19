package com.paymentprocessor.analytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI analyticsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Payment Processor — Analytics & Reporting API")
                .version("1.0.0")
                .description("Read-only report generation over the ClickHouse OLAP replica: "
                        + "scheduled reports, ad-hoc query builder, and CSV/PDF/Excel exports.")
                .license(new License().name("Proprietary")));
    }
}
