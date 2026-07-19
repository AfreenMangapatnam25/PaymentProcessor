package com.paymentprocessor.paymentservice.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds {@link RestClient} instances for outbound connector calls. Each client
 * is created with per-endpoint connect/read timeouts so a slow gateway can never
 * exhaust the request-handling threads of the payment service.
 */
@Configuration
@EnableConfigurationProperties(ConnectorProperties.class)
public class HttpClientConfig {

    /**
     * Factory bean the connectors use to build a configured {@link RestClient}
     * for a given endpoint (base URL, timeouts, API key header).
     */
    @Bean
    public RestClientFactory restClientFactory() {
        return new RestClientFactory();
    }

    /** Small factory that turns an {@link ConnectorProperties.Endpoint} into a RestClient. */
    public static class RestClientFactory {
        public RestClient build(ConnectorProperties.Endpoint ep) {
            ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                    .withConnectTimeout(Duration.ofMillis(ep.getConnectTimeoutMs()))
                    .withReadTimeout(Duration.ofMillis(ep.getReadTimeoutMs()));
            ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

            RestClient.Builder builder = RestClient.builder()
                    .baseUrl(ep.getBaseUrl())
                    .requestFactory(requestFactory);
            if (ep.getApiKey() != null && !ep.getApiKey().isBlank()) {
                builder = builder.defaultHeader("Authorization", "Bearer " + ep.getApiKey());
            }
            return builder.build();
        }
    }
}
