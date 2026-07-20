package com.paymentprocessor.userservice.application.command;

/**
 * Command to GDPR-erase a customer (crypto-shred, rule 10).
 */
public record EraseCustomerCommand(
        String customerId,
        String merchantId,
        long expectedVersion
) {
}
