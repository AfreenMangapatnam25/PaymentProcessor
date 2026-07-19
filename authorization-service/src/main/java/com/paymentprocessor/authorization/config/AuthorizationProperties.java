package com.paymentprocessor.authorization.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code authorization.*} configuration namespace.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "authorization")
public class AuthorizationProperties {

    /** Per-request gateway timeout, in seconds. */
    private int timeoutSeconds = 30;

    /** Number of retry attempts for transient gateway failures. */
    private int retryAttempts = 3;

    /** Whether Address Verification Service checks are required. */
    private boolean avsRequired = true;

    /** Whether 3-D Secure (strong customer authentication) is enabled. */
    private boolean threeDsEnabled = true;

    /** Default validity window of an authorization hold, in minutes (default 7 days). */
    private long defaultHoldMinutes = 7 * 24 * 60;

    /** Maximum number of re-authorizations permitted for a single original authorization. */
    private int maxReauthorizations = 3;

    /** Currency used when none is supplied on the request. */
    private String defaultCurrency = "USD";
}
