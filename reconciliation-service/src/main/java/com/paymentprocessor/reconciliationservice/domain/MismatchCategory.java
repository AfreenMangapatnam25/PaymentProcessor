package com.paymentprocessor.reconciliationservice.domain;

/**
 * Classification of a detected discrepancy. Each category carries a base score
 * that feeds the severity model and a default severity used as a floor.
 */
public enum MismatchCategory {
    MISSING_INTERNAL(40, SeverityLevel.HIGH),
    MISSING_EXTERNAL(25, SeverityLevel.MEDIUM),
    AMOUNT_MISMATCH(45, SeverityLevel.HIGH),
    DATE_MISMATCH(10, SeverityLevel.LOW),
    STATUS_MISMATCH(25, SeverityLevel.MEDIUM),
    DUPLICATE(30, SeverityLevel.MEDIUM),
    FX_VARIANCE(10, SeverityLevel.LOW),
    FEE_VARIANCE(25, SeverityLevel.MEDIUM),
    REFERENCE_MISMATCH(45, SeverityLevel.HIGH);

    private final int baseScore;
    private final SeverityLevel defaultSeverity;

    MismatchCategory(int baseScore, SeverityLevel defaultSeverity) {
        this.baseScore = baseScore;
        this.defaultSeverity = defaultSeverity;
    }

    public int getBaseScore() {
        return baseScore;
    }

    public SeverityLevel getDefaultSeverity() {
        return defaultSeverity;
    }
}
