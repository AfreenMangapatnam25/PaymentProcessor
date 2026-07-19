package com.paymentprocessor.userservice.application.query;

/**
 * Query to fetch one customer within a merchant scope.
 */
public record GetCustomerQuery(String customerId, String merchantId) {
}
