package com.paymentprocessor.gatewayservice.filter;

import java.time.Instant;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.paymentprocessor.gatewayservice.audit.AuditEvent;
import com.paymentprocessor.gatewayservice.audit.AuditPublisher;

import reactor.core.publisher.Mono;

/**
 * Records an audit event for every request once the response has been produced.
 * Wraps the routing filter so it observes the final status code and total latency,
 * then hands the event to {@link AuditPublisher} for asynchronous delivery.
 */
@Component
public class AuditFilter implements GlobalFilter, Ordered {

    private final AuditPublisher publisher;

    public AuditFilter(AuditPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.nanoTime();
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication() != null ? ctx.getAuthentication().getName() : "anonymous")
                .defaultIfEmpty("anonymous")
                .flatMap(principal -> chain.filter(exchange)
                        .then(Mono.fromRunnable(() -> record(exchange, principal, start))));
    }

    private void record(ServerWebExchange exchange, String principal, long startNanos) {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        var request = exchange.getRequest();
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        Integer status = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value() : 0;

        AuditEvent event = new AuditEvent(
                request.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER),
                request.getMethod().name(),
                request.getPath().value(),
                route != null ? route.getId() : "unmatched",
                principal,
                request.getHeaders().getFirst(HeaderEnrichmentFilter.MERCHANT_ID_HEADER),
                clientIp(exchange),
                status,
                durationMs,
                Instant.now().toString());

        publisher.publish(event);
    }

    private String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        // Wrap the routing filter so the recorded status/latency are the final ones.
        return Ordered.LOWEST_PRECEDENCE - 100;
    }
}
