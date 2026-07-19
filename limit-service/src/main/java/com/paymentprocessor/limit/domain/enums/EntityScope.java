package com.paymentprocessor.limit.domain.enums;

/**
 * The entity a limit configuration applies to. Precedence runs from most specific
 * (CUSTOMER) to platform-wide (GLOBAL); when several limits apply the most
 * restrictive one wins (see README "Limit Hierarchy and Precedence").
 */
public enum EntityScope {
    GLOBAL,
    MERCHANT,
    CUSTOMER,
    COUNTRY,
    CURRENCY
}
