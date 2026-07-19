package com.paymentprocessor.authorization.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Activates typed configuration property binding for the service.
 */
@Configuration
@EnableConfigurationProperties({
        AuthorizationProperties.class,
        GatewayProperties.class,
        JwtProperties.class
})
public class AppConfig {
}
