package com.paymentprocessor.authorization.domain.enums;

public enum AuthorizationType {
    /** First authorization for a payment. */
    INITIAL,
    /** A fresh authorization replacing an expired/insufficient prior one. */
    REAUTHORIZATION,
    /** An increase to an existing hold (e.g. added tip / adjusted total). */
    INCREMENTAL
}
