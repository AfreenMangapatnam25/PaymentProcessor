package com.paymentprocessor.gatewayservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.paymentprocessor.gatewayservice.config.GatewayProperties;

import reactor.core.publisher.Mono;

/**
 * Extracts an API key from the configured header into an (unauthenticated)
 * {@link ApiKeyAuthenticationToken}. Emits empty when the header is absent so the
 * request can fall through to JWT authentication.
 */
public class ApiKeyAuthenticationConverter implements ServerAuthenticationConverter {

    private final String headerName;

    public ApiKeyAuthenticationConverter(GatewayProperties properties) {
        this.headerName = properties.getApikey().getHeaderName();
    }

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String key = exchange.getRequest().getHeaders().getFirst(headerName);
        if (!StringUtils.hasText(key)) {
            return Mono.empty();
        }
        return Mono.just(new ApiKeyAuthenticationToken(key));
    }
}
