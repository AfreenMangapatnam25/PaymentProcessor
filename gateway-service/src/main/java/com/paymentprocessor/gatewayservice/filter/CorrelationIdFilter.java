package com.paymentprocessor.gatewayservice.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Guarantees every request carries a correlation id. If the client supplied one we
 * honour it; otherwise we mint a fresh UUID. The id is forwarded to downstream
 * services and echoed back to the client so a single request can be traced
 * end-to-end across the platform.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_ATTR = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        final String id = correlationId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, id)
                .build();

        exchange.getAttributes().put(CORRELATION_ID_ATTR, id);
        // Echo to the client before the response commits.
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, id);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // Run first so every later filter and log line sees the correlation id.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
