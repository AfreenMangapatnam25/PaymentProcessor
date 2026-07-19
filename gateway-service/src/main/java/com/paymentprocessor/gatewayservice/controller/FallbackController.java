package com.paymentprocessor.gatewayservice.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves fallback responses when a route's circuit breaker is open or a downstream
 * call times out. Returns a stable 503 envelope instead of leaking the underlying
 * error, so clients can retry gracefully.
 */
@RestController
@RequestMapping(value = "/fallback", produces = MediaType.APPLICATION_JSON_VALUE)
public class FallbackController {

    @RequestMapping("/{service}")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> fallback(@PathVariable String service) {
        return Map.of(
                "error", "service_unavailable",
                "message", service + " is temporarily unavailable. Please retry shortly.",
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "timestamp", Instant.now().toString());
    }
}
