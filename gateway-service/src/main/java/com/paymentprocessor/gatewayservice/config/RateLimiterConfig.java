package com.paymentprocessor.gatewayservice.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;

/**
 * Supplies the key used to bucket rate limits. Limits are enforced <em>per key</em>:
 * an API key if present, otherwise the authenticated JWT subject, otherwise the
 * caller's remote IP as a last-resort bucket for unauthenticated/public traffic.
 */
@Configuration
public class RateLimiterConfig {

    private final String apiKeyHeader;

    public RateLimiterConfig(GatewayProperties properties) {
        this.apiKeyHeader = properties.getApikey().getHeaderName();
    }

    @Bean("apiKeyResolver")
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst(apiKeyHeader);
            if (StringUtils.hasText(apiKey)) {
                // Bucket by a stable, non-secret identifier derived from the key.
                return Mono.just("apikey:" + Integer.toHexString(apiKey.hashCode()));
            }
            return ReactiveSecurityContextHolder.getContext()
                    .map(ctx -> ctx.getAuthentication())
                    .filter(Authentication::isAuthenticated)
                    .map(auth -> "sub:" + auth.getName())
                    .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + clientIp(exchange)));
        };
    }

    private String clientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }
}
