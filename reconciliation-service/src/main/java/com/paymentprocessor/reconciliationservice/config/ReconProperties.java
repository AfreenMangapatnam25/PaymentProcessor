package com.paymentprocessor.reconciliationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Externalized, type-safe configuration for the reconciliation engine (prefix {@code recon}). */
@Getter
@Setter
@ConfigurationProperties(prefix = "recon")
public class ReconProperties {

    private final Matching matching = new Matching();
    private final Severity severity = new Severity();
    private final Events events = new Events();

    @Getter
    @Setter
    public static class Matching {
        /** Absolute currency-unit tolerance treated as an exact-equivalent match (e.g. 0.01). */
        private BigDecimal amountTolerance = new BigDecimal("0.01");
        /** Percentage tolerance for FX transactions, e.g. 0.5 == 0.5%. */
        private BigDecimal amountTolerancePercent = new BigDecimal("0.5");
        /** Date window in business days for date-based matching. */
        private int dateToleranceDays = 1;
        /** Variance percentage at/below which a difference is classified as FX rounding. */
        private BigDecimal fxVariancePercent = new BigDecimal("0.5");
        /** Minimum confidence required to auto-correct without manual review. */
        private BigDecimal autoCorrectConfidence = new BigDecimal("0.95");
    }

    @Getter
    @Setter
    public static class Severity {
        /** Weight applied to the transaction amount in the severity model. */
        private double materialityWeight = 0.0005;
        /** Weight applied to the age (in days) in the severity model. */
        private double agingWeight = 2.0;
        /** Amount above which an exception routes to the senior-analyst queue. */
        private BigDecimal seniorReviewAmount = new BigDecimal("10000");
        /** Amount above which an adjustment requires dual approval. */
        private BigDecimal dualApprovalAmount = new BigDecimal("5000");
    }

    @Getter
    @Setter
    public static class Events {
        private final Topic topic = new Topic();

        @Getter
        @Setter
        public static class Topic {
            private String reconciliationCompleted = "reconciliation.completed";
            private String mismatchDetected = "reconciliation.mismatch-detected";
        }
    }
}
