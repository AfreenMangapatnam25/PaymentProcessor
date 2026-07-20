package com.paymentprocessor.reconciliationservice.service;

import com.paymentprocessor.reconciliationservice.config.ReconProperties;
import com.paymentprocessor.reconciliationservice.domain.ExceptionRecord;
import com.paymentprocessor.reconciliationservice.domain.ReviewQueue;
import com.paymentprocessor.reconciliationservice.domain.SeverityLevel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

/**
 * Computes exception severity and routing per the README model:
 * {@code Severity = (Amount x MaterialityWeight) + (Age x AgingWeight) + CategoryBaseScore},
 * clamped to 0-100, then mapped to a {@link SeverityLevel} and a manual-review {@link ReviewQueue}.
 */
@Service
public class SeverityScoringService {

    private final ReconProperties properties;

    public SeverityScoringService(ReconProperties properties) {
        this.properties = properties;
    }

    /** Populate severity score, level, review queue and SLA on the exception (in place). */
    public void score(ExceptionRecord exception) {
        double amount = exception.getAmount() == null ? 0d : exception.getAmount().abs().doubleValue();
        int ageDays = exception.getAgeDays();
        int base = exception.getCategory().getBaseScore();

        double raw = (amount * properties.getSeverity().getMaterialityWeight())
                + (ageDays * properties.getSeverity().getAgingWeight())
                + base;
        double clamped = Math.max(0d, Math.min(100d, raw));

        exception.setSeverityScore(BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP));
        exception.setSeverityLevel(highest(SeverityLevel.fromScore(clamped),
                exception.getCategory().getDefaultSeverity()));

        ReviewQueue queue = routeQueue(exception);
        exception.setReviewQueue(queue);
        exception.setSlaDueAt(exception.getDetectedAt().plus(queue.getSlaHours(), ChronoUnit.HOURS));
    }

    private ReviewQueue routeQueue(ExceptionRecord exception) {
        BigDecimal amount = exception.getAmount() == null ? BigDecimal.ZERO : exception.getAmount().abs();
        if (amount.compareTo(properties.getSeverity().getSeniorReviewAmount()) > 0) {
            return ReviewQueue.SENIOR_ANALYST;
        }
        if (exception.getAgeDays() > 5) {
            return ReviewQueue.ESCALATED;
        }
        if (exception.getSeverityLevel() == SeverityLevel.CRITICAL) {
            return ReviewQueue.SENIOR_ANALYST;
        }
        return ReviewQueue.OPERATIONS_ANALYST;
    }

    private SeverityLevel highest(SeverityLevel a, SeverityLevel b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
