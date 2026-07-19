package com.paymentprocessor.settlementservice.exception;

/**
 * Raised when a call to the Ledger Service fails (network error, timeout, or
 * an error response) for a posting that is not payout-scoped (settlement,
 * reserve, reversal, or adjustment postings). These postings happen inside an
 * {@code @Transactional} service method, so throwing here rolls back the
 * caller's local state change and leaves the batch/reserve/etc. in its prior
 * status; the periodic scheduler (settlement.scheduler.cycle-cron /
 * retry-cron) will naturally re-attempt on its next sweep.
 *
 * <p>Money movement failures are serious: this is intentionally loud (logged
 * at ERROR by the caller) rather than swallowed.
 */
public class LedgerServiceException extends SettlementException {

    public LedgerServiceException(String message) {
        super("LEDGER_SERVICE_ERROR", message);
    }

    public LedgerServiceException(String message, Throwable cause) {
        super("LEDGER_SERVICE_ERROR", message, cause);
    }
}
