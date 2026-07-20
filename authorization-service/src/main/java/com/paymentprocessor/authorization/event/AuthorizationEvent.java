package com.paymentprocessor.authorization.event;

import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;
import com.paymentprocessor.authorization.domain.payment.AuthorizationRecord;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitted whenever an authorization changes state so that Payment, Ledger, Settlement,
 * Notification and Reporting services stay synchronized.
 */
@Builder
public record AuthorizationEvent(
        UUID eventId,
        String eventType,
        UUID authorizationId,
        String paymentReference,
        String merchantId,
        AuthorizationStatus status,
        BigDecimal approvedAmount,
        BigDecimal capturedAmount,
        String currency,
        String authorizationCode,
        Instant occurredAt
) {
    public static AuthorizationEvent of(String eventType, AuthorizationRecord r) {
        return AuthorizationEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .authorizationId(r.getId())
                .paymentReference(r.getPaymentReference())
                .merchantId(r.getMerchantId())
                .status(r.getStatus())
                .approvedAmount(r.getApprovedAmount())
                .capturedAmount(r.getCapturedAmount())
                .currency(r.getCurrency())
                .authorizationCode(r.getAuthorizationCode())
                .occurredAt(Instant.now())
                .build();
    }

    public static final class Types {
        public static final String AUTHORIZED = "AuthorizationApproved";
        public static final String DECLINED = "AuthorizationDeclined";
        public static final String PARTIALLY_APPROVED = "AuthorizationPartiallyApproved";
        public static final String AUTHENTICATION_REQUIRED = "AuthorizationRequiresAuthentication";
        public static final String CAPTURED = "AuthorizationCaptured";
        public static final String REVERSED = "AuthorizationReversed";
        public static final String EXPIRED = "AuthorizationExpired";
        public static final String REAUTHORIZED = "AuthorizationReauthorized";

        private Types() {
        }
    }
}
