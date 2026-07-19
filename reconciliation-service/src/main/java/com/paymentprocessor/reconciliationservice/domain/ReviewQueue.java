package com.paymentprocessor.reconciliationservice.domain;

/** Manual-review routing queue with an associated SLA (in hours). */
public enum ReviewQueue {
    SENIOR_ANALYST(4),
    ESCALATED(24),
    FRAUD_INVESTIGATION(2),
    COMPLIANCE(4),
    OPERATIONS_ANALYST(48);

    private final int slaHours;

    ReviewQueue(int slaHours) {
        this.slaHours = slaHours;
    }

    public int getSlaHours() {
        return slaHours;
    }
}
