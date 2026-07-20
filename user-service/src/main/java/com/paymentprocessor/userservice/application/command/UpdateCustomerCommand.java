package com.paymentprocessor.userservice.application.command;

import java.util.Map;

/**
 * Command to update a customer's contact details and metadata.
 */
public record UpdateCustomerCommand(
        String customerId,
        String merchantId,
        String email,
        String fullName,
        String phone,
        Map<String, String> metadata,
        long expectedVersion
) {
}
