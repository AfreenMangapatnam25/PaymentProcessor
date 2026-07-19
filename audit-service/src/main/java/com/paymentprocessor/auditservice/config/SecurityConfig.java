package com.paymentprocessor.auditservice.config;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * Registers the {@link ApiKeyAuthFilter} ahead of the dispatcher servlet when
 * {@code audit.security.enabled=true}.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilter(AuditProperties props) {
        FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>();

        Set<String> keys = props.getSecurity().getApiKeys().stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (!props.getSecurity().isEnabled()) {
            log.warn("API-key authentication is DISABLED (audit.security.enabled=false). "
                    + "This must only be used in local/dev environments.");
            registration.setEnabled(false);
            registration.setFilter(new ApiKeyAuthFilter(keys));
            return registration;
        }

        if (keys.isEmpty()) {
            throw new IllegalStateException(
                    "audit.security.enabled=true but no audit.security.api-keys are configured");
        }

        registration.setFilter(new ApiKeyAuthFilter(keys));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("apiKeyAuthFilter");
        log.info("API-key authentication enabled with {} configured key(s).", keys.size());
        return registration;
    }
}
