package com.paymentprocessor.reconciliationservice.event;

import com.paymentprocessor.reconciliationservice.domain.ExceptionRecord;
import com.paymentprocessor.reconciliationservice.domain.ReconRun;

/** Publishes reconciliation domain events to downstream consumers. */
public interface ReconEventPublisher {

    void publishReconciliationCompleted(ReconRun run);

    void publishMismatchDetected(ExceptionRecord exception);
}
