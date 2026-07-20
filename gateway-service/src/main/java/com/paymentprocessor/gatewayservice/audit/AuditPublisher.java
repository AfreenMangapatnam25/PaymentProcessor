package com.paymentprocessor.gatewayservice.audit;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.paymentprocessor.gatewayservice.config.GatewayProperties;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Ships audit events to the audit-service without blocking the request. Delivery is
 * best-effort: a failure is logged but never propagated to the client, because the
 * gateway must keep serving traffic even if auditing is degraded.
 */
@Component
public class AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditPublisher.class);

    private final GatewayProperties.Audit config;
    private final WebClient webClient;

    public AuditPublisher(GatewayProperties properties, WebClient.Builder webClientBuilder) {
        this.config = properties.getAudit();
        this.webClient = webClientBuilder
                .baseUrl(config.getUri())
                .build();
    }

    /** Fire-and-forget publish; returns immediately. */
    public void publish(AuditEvent event) {
        if (!config.isEnabled()) {
            return;
        }
        webClient.post()
                .uri(config.getPath())
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(2))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(100)))
                .doOnError(err -> log.warn("Failed to publish audit event for correlationId={}: {}",
                        event.correlationId(), err.toString()))
                .onErrorResume(err -> Mono.empty())
                .subscribe();
    }
}
