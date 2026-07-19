package com.paymentprocessor.authorization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Authorization Service.
 *
 * <p>A hybrid service that provides two related capabilities for the payment platform:
 * <ul>
 *   <li><b>Payment authorization</b> &mdash; obtains APPROVE/DECLINE verdicts from external
 *       payment gateways / card networks (Stripe manual-capture PaymentIntents), and manages
 *       holds, capture, reversal (void) and re-authorization.</li>
 *   <li><b>Access control (RBAC/ABAC)</b> &mdash; the platform policy enforcement point that
 *       decides what an authenticated identity is permitted to do, backed by a policy
 *       evaluation engine, scope validation and a permission cache.</li>
 * </ul>
 *
 * <p>In modern Spring Cloud (2023.x) the presence of the Eureka client starter is sufficient;
 * {@code @EnableEurekaClient} has been removed. {@link EnableDiscoveryClient} remains optional
 * but is kept here for explicitness.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableAsync
@EnableScheduling
public class AuthorizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizationServiceApplication.class, args);
    }
}
