package com.paymentprocessor.settlementservice.integration.ledger;

/**
 * Gateway to the double-entry Ledger Service. Every money movement in the
 * settlement lifecycle posts a journal and stores the returned journal id for
 * reconciliation and audit.
 */
public interface LedgerClient {

    /**
     * Posts the settlement of a batch: reduces the merchant settlement liability
     * and records fee, reserve, and cash-outflow entries.
     *
     * @return the ledger journal id
     */
    String postSettlement(String batchId, String merchantId, String currency,
                          long netMinor, long feesMinor, long reserveMinor);

    /** Posts a rolling-reserve hold. */
    String postReserveHold(String reserveId, String merchantId, String currency, long amountMinor);

    /** Posts a reserve release back into available funds. */
    String postReserveRelease(String reserveId, String merchantId, String currency, long amountMinor);

    /** Posts the cash outflow when a payout is confirmed paid. */
    String postPayout(String payoutId, String merchantId, String currency, long amountMinor);

    /** Posts a compensating entry when funds are returned by the bank. */
    String postPayoutReturn(String payoutId, String merchantId, String currency, long amountMinor);

    /** Posts a compensating entry for a settlement reversal (fund recovery). */
    String postReversal(String batchId, String merchantId, String currency, long amountMinor, String reason);

    /** Posts a manual adjustment. */
    String postAdjustment(String adjustmentId, String merchantId, String currency, long signedAmountMinor);
}
