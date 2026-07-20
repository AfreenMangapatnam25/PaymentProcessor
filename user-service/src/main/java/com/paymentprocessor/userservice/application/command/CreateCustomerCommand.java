package com.paymentprocessor.userservice.application.command;

import java.util.Map;

/**
 * Command to create a merchant-scoped customer. {@code merchantId} is resolved
 * from the caller's token at the boundary, never trusted from the body.
 */
public record CreateCustomerCommand(
        String merchantId,
        String email,
        String fullName,
        String phone,
        String externalRef,
        String userId,
        Map<String, String> metadata
) {
}
