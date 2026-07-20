package com.paymentprocessor.ledgerservice.web.error;

import org.springframework.http.HttpStatus;

/** Debits do not equal credits within a journal (422). */
public class UnbalancedJournalException extends LedgerException {

    public UnbalancedJournalException(String message) {
        super("UNBALANCED_JOURNAL", HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
