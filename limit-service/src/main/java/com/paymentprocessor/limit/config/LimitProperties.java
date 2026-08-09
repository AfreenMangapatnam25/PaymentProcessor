package com.paymentprocessor.limit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Externalised limit-service tuning, bound from the {@code limit.*} keys in the
 * application configuration.
 */
@Component
@ConfigurationProperties(prefix = "limit")
@Getter
@Setter
public class LimitProperties {

    private BigDecimal dailyTransactionLimit = new BigDecimal("10000.00");
    private BigDecimal monthlyTransactionLimit = new BigDecimal("100000.00");
    private BigDecimal perTransactionLimit = new BigDecimal("50000.00");
    private int velocityWindowMinutes = 60;
    private int velocityMaxTransactions = 20;

    /** How long a reservation is held before the sweeper auto-releases it. */
    private int reservationTtlMinutes = 15;

    private Events events = new Events();

    @Getter
    @Setter
    public static class Events {
        private String reservedTopic = "limitservicetopic";
        private String releasedTopic = "limitservicetopic";
        private String exceededTopic = "limitservicetopic";
    }
}
