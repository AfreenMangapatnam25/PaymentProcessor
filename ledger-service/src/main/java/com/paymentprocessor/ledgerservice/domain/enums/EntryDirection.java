package com.paymentprocessor.ledgerservice.domain.enums;

/**
 * The direction of a single ledger entry line.
 */
public enum EntryDirection {
    DEBIT,
    CREDIT;

    /** The opposite direction, used when constructing reversal entries. */
    public EntryDirection opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
