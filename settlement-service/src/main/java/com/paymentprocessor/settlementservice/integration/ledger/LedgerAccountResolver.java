package com.paymentprocessor.settlementservice.integration.ledger;

/**
 * Resolves ledger account ids from well-known purposes via ledger-service.
 */
public interface LedgerAccountResolver {

    String platformCash();

    String platformFeeRevenue();

    String platformPayoutPayable();

    String platformAdjustmentExpense();

    String merchantSettlementLiability(String merchantId, String currency);

    String merchantReserve(String merchantId, String currency);
}
