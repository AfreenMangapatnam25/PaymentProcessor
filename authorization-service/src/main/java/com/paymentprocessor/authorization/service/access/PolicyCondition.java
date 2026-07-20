package com.paymentprocessor.authorization.service.access;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single ABAC clause: {@code attribute operator value}. Clauses within a policy are AND-combined.
 * Example: {@code {"attribute":"subject.kyc_status","operator":"EQUALS","value":"VERIFIED"}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolicyCondition(String attribute, Operator operator, Object value) {

    public enum Operator {
        EQUALS, NOT_EQUALS, GT, GTE, LT, LTE, IN, CONTAINS
    }
}
