package com.paymentprocessor.reconciliationservice.domain;

/** The reconciliation domain / external counterparty class a run targets. */
public enum ReconType {
    BANK,
    ACQUIRER,
    CARD_NETWORK,
    GATEWAY,
    LEDGER,
    SETTLEMENT
}
