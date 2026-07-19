package com.paymentprocessor.gatewayservice.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.paymentprocessor.gatewayservice.config.GatewayProperties;

import reactor.core.publisher.Mono;

/**
 * Rejects oversized request bodies before they are proxied to a backend, based on
 * the declared {@code Content-Length}. This protects downstream services from
 * memory-exhaustion attacks. Streaming requests without a Content-Length are
 * additionally bounded by the Netty limits configured on the server.
 */
@Component
public class RequestSizeFilter implements GlobalFilter, Ordered {

    private final long maxBytes;

    public RequestSizeFilter(GatewayProperties properties) {
        this.maxBytes = properties.getMaxRequestBodyBytes();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long contentLength = exchange.getRequest().getHeaders().getContentLength();
        if (contentLength > maxBytes) {
            exchange.getResponse().setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // After correlation id, before routing/auth work.
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
