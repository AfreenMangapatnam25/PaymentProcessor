package com.paymentprocessor.reconciliationservice.domain;

/** Supported external statement file formats. */
public enum StatementFormat {
    MT940,
    CAMT053,
    BAI2,
    CSV,
    XML,
    JSON
}
