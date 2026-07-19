package com.paymentprocessor.merchantservice.common.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables Spring Data JPA auditing so BaseEntity timestamps are populated automatically. */
@Configuration
@EnableJpaAuditing
public class AuditConfig {
}
