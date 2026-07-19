package com.paymentprocessor.authorization.gateway.stripe;

import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;

/**
 * Maps Stripe PaymentIntent statuses to the service's {@link AuthorizationStatus} vocabulary.
 */
final class StripeStatusMapper {

    private StripeStatusMapper() {
    }

    static AuthorizationStatus fromPaymentIntentStatus(String status) {
        if (status == null) {
            return AuthorizationStatus.FAILED;
        }
        return switch (status) {
            case "requires_capture" -> AuthorizationStatus.APPROVED;
            case "requires_action", "requires_confirmation" -> AuthorizationStatus.REQUIRES_AUTHENTICATION;
            case "succeeded" -> AuthorizationStatus.CAPTURED;
            case "processing" -> AuthorizationStatus.PENDING;
            case "canceled" -> AuthorizationStatus.REVERSED;
            case "requires_payment_method" -> AuthorizationStatus.DECLINED;
            default -> AuthorizationStatus.FAILED;
        };
    }
}
