package com.paymentprocessor.disputeservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Builds named {@link WebClient} instances for each downstream service the
 * Dispute Service integrates with. Base URLs are externalised so they can be
 * overridden per environment (see application.yml).
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient paymentServiceWebClient(
            @Value("${payment.service.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient ledgerServiceWebClient(
            @Value("${ledger.service.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient settlementServiceWebClient(
            @Value("${settlement.service.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public WebClient notificationServiceWebClient(
            @Value("${notification.service.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
