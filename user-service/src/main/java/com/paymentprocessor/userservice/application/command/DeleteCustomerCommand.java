package com.paymentprocessor.userservice.application.command;

/**
 * Command to soft-delete a customer (rule 9).
 */
public record DeleteCustomerCommand(
        String customerId,
        String merchantId,
        long expectedVersion
) {
}
