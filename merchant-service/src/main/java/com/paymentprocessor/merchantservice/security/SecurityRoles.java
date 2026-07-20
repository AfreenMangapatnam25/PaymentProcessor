package com.paymentprocessor.merchantservice.security;

/** Central definition of authority names used across the service. */
public final class SecurityRoles {
    public static final String ADMIN = "ADMIN";
    public static final String MERCHANT = "MERCHANT";
    public static final String READ = "READ";
    public static final String WRITE = "WRITE";

    private SecurityRoles() {
    }
}
