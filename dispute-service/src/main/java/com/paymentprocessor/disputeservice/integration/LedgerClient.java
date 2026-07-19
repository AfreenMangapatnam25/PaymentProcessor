package com.paymentprocessor.disputeservice.integration;

/**
 * Outbound port to the Ledger Service, which owns the double-entry journal.
 * The Dispute Service posts chargeback debits, representment credits and fee
 * expenses but never mutates ledger records directly.
 */
public interface LedgerClient {

    /**
     * Posts the chargeback debit (disputed amount + fee) against the merchant.
     *
     * @return the ledger journal id backing the posting
     */
    String postChargebackDebit(String disputeId, String merchantId,
                               long amountMinor, long feeMinor, String currency);

    /**
     * Reverses a previously posted chargeback when a dispute is won.
     *
     * @return the reversal journal id
     */
    String reverseChargeback(String disputeId, String originalJournalId);

    /**
     * Confirms the chargeback loss as final (dispute lost / accepted), moving the
     * held funds from reserve to platform revenue.
     *
     * @return the settlement journal id
     */
    String finalizeLoss(String disputeId, String originalJournalId);

    /**
     * Posts a standalone fee expense (e.g. arbitration filing fee).
     *
     * @return the fee journal id
     */
    String postFee(String disputeId, long feeMinor, String currency, String description);
}
