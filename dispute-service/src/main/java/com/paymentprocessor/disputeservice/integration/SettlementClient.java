package com.paymentprocessor.disputeservice.integration;

/**
 * Outbound port to the Settlement Service for recovering chargeback funds from
 * a merchant's settlements / reserve and releasing them again when a dispute is
 * won.
 */
public interface SettlementClient {

    /** Recovers the disputed amount + fee from the merchant's next settlement or reserve. */
    String recoverFromSettlement(String merchantId, String disputeId,
                                 long amountMinor, String currency);

    /** Releases previously recovered funds back to the merchant on a win. */
    void releaseToMerchant(String merchantId, String disputeId,
                           long amountMinor, String currency);

    /** Adjusts the merchant's rolling reserve to the given tier percentage. */
    void adjustReserve(String merchantId, int reservePercentage, int rollingDays);
}
