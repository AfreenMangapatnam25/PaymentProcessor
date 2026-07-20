package com.paymentprocessor.settlementservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient instances for the two outbound service integrations settlement-service owns:
 * posting double-entry journals to the Ledger Service, and reading merchant settlement
 * configuration from the Merchant Service.
 */
@Configuration
public class IntegrationWebClientConfig {

    @Bean
    public WebClient ledgerWebClient(@Value("${settlement.integration.ledger.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient merchantWebClient(@Value("${settlement.integration.merchant.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
