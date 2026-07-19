package com.paymentprocessor.reconciliationservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables reconciliation configuration properties binding. */
@Configuration
@EnableConfigurationProperties(ReconProperties.class)
public class ReconConfiguration {
}
