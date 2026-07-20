package com.paymentprocessor.settlementservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution for the settlement cycle,
 * retry sweep, and reserve-release jobs.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
