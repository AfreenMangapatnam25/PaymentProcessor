package com.paymentprocessor.userservice.common.model;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/*
 * Bean name is set explicitly to "userRequestContextFilter".
 *
 * Spring Boot's WebMvcAutoConfiguration already registers a bean literally named
 * "requestContextFilter" (its own org.springframework.web.filter.RequestContextFilter).
 * Because this class has the same simple name, the default @Component naming strategy
 * produces the same bean name, and with bean-definition overriding disabled (the Boot
 * default) startup fails with:
 *
 *   The bean 'requestContextFilter' ... could not be registered. A bean with that name
 *   has already been defined ... and overriding is disabled.
 *
 * Naming this bean explicitly is preferable to setting
 * spring.main.allow-bean-definition-overriding=true, which would silently let one bean
 * clobber the other here and anywhere else in the application.
 */
@Component("userRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {

    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    private static final String HEADER_MERCHANT_ID = "X-Merchant-Id";
    private static final String HEADER_IDENTITY_ID = "X-Identity-Id";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    private final ThreadLocal<RequestContext> contextHolder = new ThreadLocal<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Extract headers
            String correlationIdHeader = request.getHeader(HEADER_CORRELATION_ID);
            String merchantId = request.getHeader(HEADER_MERCHANT_ID);
            String identityId = request.getHeader(HEADER_IDENTITY_ID);
            String requestId = request.getHeader(HEADER_REQUEST_ID);

            // Create correlation ID
            CorrelationId correlationId;
            if (correlationIdHeader != null && CorrelationId.isValid(correlationIdHeader)) {
                correlationId = CorrelationId.fromString(correlationIdHeader);
            } else {
                correlationId = CorrelationId.generate();
            }

            // Create context
            RequestContext context;
            if (requestId != null) {
                context = RequestContext.of(
                        requestId,
                        correlationId,
                        merchantId,
                        identityId,
                        Instant.now()
                );
            } else {
                context = RequestContext.create(correlationId, merchantId, identityId);
            }

            // Store in ThreadLocal
            contextHolder.set(context);

            // Populate MDC for logging
            populateMDC(context);

            // Add correlation ID and request ID to response headers
            response.setHeader(HEADER_CORRELATION_ID, correlationId.value());
            response.setHeader(HEADER_REQUEST_ID, context.requestId());

            // Continue filter chain
            filterChain.doFilter(request, response);

        } finally {
            // Clean up
            contextHolder.remove();
            MDC.clear();
        }
    }

    private void populateMDC(RequestContext context) {
        MDC.put("requestId", context.requestId());
        MDC.put("correlationId", context.getCorrelationIdAsString());
        MDC.put("merchantId", context.merchantId());
        MDC.put("identityId", context.identityId());
        MDC.put("requestTime", context.requestTime().toString());
    }

    /**
     * Gets the current request context from ThreadLocal.
     */
    public RequestContext getCurrentContext() {
        return contextHolder.get();
    }

    /**
     * Gets the current request context, throwing an exception if not present.
     */
    public RequestContext requireCurrentContext() {
        RequestContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("No request context available");
        }
        return context;
    }

    /**
     * Gets the current correlation ID from the request context.
     */
    public CorrelationId getCurrentCorrelationId() {
        RequestContext context = getCurrentContext();
        return context != null ? context.correlationId() : null;
    }
}