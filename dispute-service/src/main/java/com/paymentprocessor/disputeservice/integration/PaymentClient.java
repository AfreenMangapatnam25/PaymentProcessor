package com.paymentprocessor.disputeservice.integration;

import java.util.Optional;

/**
 * Outbound port to the Payment Service, which owns transaction records. Used to
 * flag disputed transactions and to pull the transaction detail needed when
 * assembling evidence.
 */
public interface PaymentClient {

    /** Marks the original transaction as disputed. */
    void flagTransactionDisputed(String transactionId, String disputeId);

    /** Clears the disputed flag when a dispute is won or withdrawn. */
    void clearDisputedFlag(String transactionId, String disputeId);

    /** Retrieves transaction detail used for evidence assembly, if available. */
    Optional<TransactionDetail> getTransactionDetail(String transactionId);

    /**
     * A read-only projection of transaction data owned by the Payment Service.
     */
    record TransactionDetail(String transactionId, long amountMinor, String currency,
                             String avsResult, String cvvResult, boolean threeDsAuthenticated,
                             String authorizationCode) {
    }
}
