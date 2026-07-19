package com.paymentprocessor.authorization.dto.payment;

import com.paymentprocessor.authorization.domain.enums.AuthorizationStatus;
import com.paymentprocessor.authorization.domain.enums.AuthorizationType;
import com.paymentprocessor.authorization.domain.enums.AvsResult;
import com.paymentprocessor.authorization.domain.enums.CardNetwork;
import com.paymentprocessor.authorization.domain.enums.CvvResult;
import com.paymentprocessor.authorization.domain.payment.AuthorizationRecord;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public view of an authorization returned by the API.
 */
@Builder
public record AuthorizationResponse(
        UUID authorizationId,
        AuthorizationStatus status,
        AuthorizationType type,
        String paymentReference,
        String merchantId,
        String customerId,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        BigDecimal capturedAmount,
        String currency,
        String authorizationCode,
        String networkReferenceId,
        String responseCode,
        String gatewayResponseMessage,
        CardNetwork cardNetwork,
        String cardLast4,
        AvsResult avsResult,
        CvvResult cvvResult,
        boolean requiresAuthentication,
        String authenticationUrl,
        UUID originalAuthorizationId,
        int reauthorizationCount,
        Instant expiresAt,
        Instant authorizedAt,
        Instant capturedAt,
        Instant reversedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AuthorizationResponse from(AuthorizationRecord r) {
        return AuthorizationResponse.builder()
                .authorizationId(r.getId())
                .status(r.getStatus())
                .type(r.getType())
                .paymentReference(r.getPaymentReference())
                .merchantId(r.getMerchantId())
                .customerId(r.getCustomerId())
                .requestedAmount(r.getRequestedAmount())
                .approvedAmount(r.getApprovedAmount())
                .capturedAmount(r.getCapturedAmount())
                .currency(r.getCurrency())
                .authorizationCode(r.getAuthorizationCode())
                .networkReferenceId(r.getNetworkReferenceId())
                .responseCode(r.getResponseCode())
                .gatewayResponseMessage(r.getGatewayResponseMessage())
                .cardNetwork(r.getCardNetwork())
                .cardLast4(r.getCardLast4())
                .avsResult(r.getAvsResult())
                .cvvResult(r.getCvvResult())
                .requiresAuthentication(r.isRequiresAuthentication())
                .authenticationUrl(r.getAuthenticationUrl())
                .originalAuthorizationId(r.getOriginalAuthorizationId())
                .reauthorizationCount(r.getReauthorizationCount())
                .expiresAt(r.getExpiresAt())
                .authorizedAt(r.getAuthorizedAt())
                .capturedAt(r.getCapturedAt())
                .reversedAt(r.getReversedAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
