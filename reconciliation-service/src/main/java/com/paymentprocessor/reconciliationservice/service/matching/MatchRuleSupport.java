package com.paymentprocessor.reconciliationservice.service.matching;

import com.paymentprocessor.reconciliationservice.domain.ReconRecord;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

/** Shared comparison helpers for match rules. */
final class MatchRuleSupport {

    private MatchRuleSupport() {
    }

    static boolean amountsEqual(ReconRecord a, ReconRecord b) {
        return a.getAmount() != null && b.getAmount() != null
                && a.getAmount().compareTo(b.getAmount()) == 0;
    }

    static boolean amountsWithinAbsolute(ReconRecord a, ReconRecord b, BigDecimal tolerance) {
        if (a.getAmount() == null || b.getAmount() == null) {
            return false;
        }
        return a.getAmount().subtract(b.getAmount()).abs().compareTo(tolerance) <= 0;
    }

    static boolean amountsWithinPercent(ReconRecord a, ReconRecord b, BigDecimal percent) {
        if (a.getAmount() == null || b.getAmount() == null) {
            return false;
        }
        BigDecimal base = a.getAmount().abs();
        if (base.signum() == 0) {
            return b.getAmount().signum() == 0;
        }
        BigDecimal diff = a.getAmount().subtract(b.getAmount()).abs();
        BigDecimal diffPercent = diff.multiply(BigDecimal.valueOf(100))
                .divide(base, 6, java.math.RoundingMode.HALF_UP);
        return diffPercent.compareTo(percent) <= 0;
    }

    static BigDecimal amountVariance(ReconRecord a, ReconRecord b) {
        if (a.getAmount() == null || b.getAmount() == null) {
            return null;
        }
        return a.getAmount().subtract(b.getAmount());
    }

    static Integer dateVarianceDays(ReconRecord a, ReconRecord b) {
        if (a.getTransactionDate() == null || b.getTransactionDate() == null) {
            return null;
        }
        return (int) Math.abs(ChronoUnit.DAYS.between(a.getTransactionDate(), b.getTransactionDate()));
    }

    static boolean referencesEqual(String a, String b) {
        return a != null && !a.isBlank() && a.equalsIgnoreCase(b);
    }

    static boolean valuesEqual(String a, String b) {
        return a != null && !a.isBlank() && a.equalsIgnoreCase(b);
    }
}
